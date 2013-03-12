/*
 * Copyright 2013 glenn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.accounted4.money.loan;

import com.accounted4.money.Money;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.joda.time.LocalDate;

/**
 * This class provides a static interface callable by the iReport tool
 * to return a mock List of Collections. This is in order to support
 * report development.
 * 
 * @author glenn
 */
public class JasperReportDataSourceAmortizationSchedule {
    
    // For calling by iReport
    public static Collection<ScheduledPayment> getSamplePaymentCollection() {
        
        List<ScheduledPayment> paymentList = new ArrayList<>();

        AmortizationAttributes amAttrs = new AmortizationAttributes();
        amAttrs.setLoanAmount( new Money("20000.00") );
        amAttrs.setRegularPayment( new Money("0") );     // monthly payment to be made, assumed monthly
        amAttrs.setStartDate(new LocalDate(2013, 1, 8));
        amAttrs.setAdjustmentDate(new LocalDate(2013, 1, 15));
        amAttrs.setTermInMonths(12);
        amAttrs.setInterestOnly(false);
        amAttrs.setAmortizationPeriodMonths(25 * 12);
        amAttrs.setCompoundingPeriodsPerYear(2);
        amAttrs.setInterestRate(12.);
    
        Iterator<ScheduledPayment> payments = AmortizationCalculator.getPayments(amAttrs);
        while (payments.hasNext()) {
            paymentList.add(payments.next());
        }

        return paymentList;
        
    }
    
    public static void main(String[] args) {
        
        Collection<ScheduledPayment> samplePaymentCollection =
                JasperReportDataSourceAmortizationSchedule.getSamplePaymentCollection();
        
        for (ScheduledPayment payment : samplePaymentCollection) {
            System.out.println(payment.toString());
        }
        
    }
    
}
