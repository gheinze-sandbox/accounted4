package com.accounted4.midtier.service;


import com.accounted4.money.Money;
import com.accounted4.money.loan.AmortizationAttributes;
import com.accounted4.money.loan.AmortizationCalculator;
import com.accounted4.money.loan.ScheduledPayment;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.PreDestroy;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Utilities to support an amortization calculator.
 * 
 * @author Glenn Heinze
 */
@Service
public class AmortizationService {

    
    // The compiled Jasper Report for an Amortization schedule
    // TODO: reports should be lazy loaded and cached after first use
    @Autowired
    private AmortizationScheduleJasperReport reportFactory;
    
    // When requesting a pdf file, the client makes two calls: the first sends
    // the required attributes and receives an id in response.
    // The id is saved in a cache associated with the session allowing the
    // user to request the document by id
    
    // TODO: build the WeakHashmap based on the JSessionId, not really correct on the IdBean
    // since the IdBean will go out of scope immediately.
    private static final WeakHashMap<IdBean, AmortizationAttributes> AM_SCHEDULE_CACHE
            = new WeakHashMap<>();
    
    
    public List<ScheduledPayment> getAmortizationSchedule(final AmortizationAttributes amAttrs) {

        List<ScheduledPayment> paymentList = new ArrayList<>();

        Iterator<ScheduledPayment> payments = AmortizationCalculator.getPayments(amAttrs);
        while (payments.hasNext()) {
            paymentList.add(payments.next());
        }

        return paymentList;

    }
    
    public IdBean cacheSchedule(final String sessionId, final AmortizationAttributes amAttrs) {
        String id = UUID.randomUUID().toString() + sessionId;
        IdBean idBean = new IdBean(id);
        AM_SCHEDULE_CACHE.put(idBean, amAttrs);
        return idBean;
    }

    private static final int MONTHS_PER_YEAR = 12;
    
    public void generateAmortizationSchedulePdf(
            final IdBean idBean,
            final OutputStream outputStream
    ) throws JRException, IOException {
        
        AmortizationAttributes amAttrs = AM_SCHEDULE_CACHE.remove(idBean);
        
        List<ScheduledPayment> payments = getAmortizationSchedule(amAttrs);
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(payments);

        // TODO: name, title, etc should be configurable parameters as well
        Map<String, Object> customParameters = new HashMap<>();
        customParameters.put("amount", amAttrs.getLoanAmount());
        customParameters.put("rate", amAttrs.getInterestRate());
        customParameters.put("monthlyPayment", amAttrs.getRegularPayment());
        customParameters.put("term", amAttrs.getTermInMonths());
        if (!amAttrs.isInterestOnly()) {
            customParameters.put("amortizationYears", amAttrs.getAmortizationPeriodMonths() / MONTHS_PER_YEAR);
            customParameters.put("amortizationMonths", amAttrs.getAmortizationPeriodMonths() % MONTHS_PER_YEAR);
            customParameters.put("compoundPeriod", amAttrs.getCompoundingPeriodsPerYear());
        }
        customParameters.put("mortgagee", "Accounted4");
        
        
        JasperReport compiledReport = reportFactory.getCompiledReport();

        JasperPrint jasperPrint = JasperFillManager.fillReport(compiledReport, customParameters, ds);

        JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
        
//        File pdfFile = File.createTempFile("amSchedule", ".pdf");
//        JasperExportManager.exportReportToPdfFile(jasperPrint, pdfFile.getCanonicalPath());
        
    }

    
    public Money getMonthlyPayment(final AmortizationAttributes amAttrs) {
        return AmortizationCalculator.getMonthlyPayment(amAttrs);
    }

    @PreDestroy
    public void clearReportCache() {
        AM_SCHEDULE_CACHE.clear();
    }
    
}
