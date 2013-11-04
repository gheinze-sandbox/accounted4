package com.accounted4.midtier.service;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.PostConstruct;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The Jasper report for the amortization schedule only needs to be compiled
 * once, after which the compiled report can be used by the application to 
 * generate actual pdfs.
 * 
 * @author glenn
 */
@Component
public class AmortizationScheduleJasperReport {

    private JasperReport report;
    
    @PostConstruct
    public void init() throws IOException, JRException {
        ClassPathResource resource = new ClassPathResource("com/accounted4/midtier/reports/AmortizationSchedule.jasper");
        try (InputStream resourceInputStream = resource.getInputStream()) {
            report = (JasperReport) JRLoader.loadObject(resourceInputStream);
        }
    }
    
    public JasperReport getCompiledReport() {
        return report;
    }
    
}
