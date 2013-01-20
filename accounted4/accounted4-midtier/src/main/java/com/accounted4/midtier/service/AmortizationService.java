package com.accounted4.midtier.service;


import com.accounted4.money.Money;
import com.accounted4.money.loan.AmortizationAttributes;
import com.accounted4.money.loan.AmortizationCalculator;
import com.accounted4.money.loan.ScheduledPayment;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Service;


/**
 * Utilities to support an amortization calculator
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
@Service
public class AmortizationService {

    
    
    public List<ScheduledPayment> getAmortizationSchedule(AmortizationAttributes amAttrs) {

        List<ScheduledPayment> paymentList = new ArrayList<>();

        Iterator<ScheduledPayment> payments = AmortizationCalculator.getPayments(amAttrs);
        while (payments.hasNext()) {
            paymentList.add(payments.next());
        }

        return paymentList;

    }


    
    public Money getMonthlyPayment(AmortizationAttributes amAttrs) {
        return AmortizationCalculator.getMonthlyPayment(amAttrs);
    }

    
}
