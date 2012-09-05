/*
 * Copyright 2012 Glenn Heinze <glenn@gheinze.com>.
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Iterator;
import org.joda.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class AmortizationCalculatorTest {
    
    public AmortizationCalculatorTest() {
    }

    /**
     * Test of getPayments method, of class AmortizationCalculator.
     */
    @Test
    public void testGetPaymentsInterestOnly() {
        System.out.println("getPaymentsInterestOnly");
        
        Money amount = new Money("100.00");
        double rate = 12.0;

        AmortizationAttributes terms = new AmortizationAttributes();
        terms.setInterestOnly(true);
        terms.setLoanAmount(amount);
        terms.setTermInMonths(12);
        terms.setInterestRate(rate);
        double interestOnlyMonthlyPayment = AmortizationCalculator.getInterestOnlyMonthlyPayment(amount.getAmount().doubleValue(), rate);
        terms.setRegularPayment(new Money(BigDecimal.valueOf(interestOnlyMonthlyPayment)));
        
        LocalDate today = new LocalDate();
        LocalDate adjDate = new LocalDate();
        
        int dayOfMonth = today.getDayOfMonth();
        if (1 != dayOfMonth) {
            if (dayOfMonth > 15) {
              adjDate = new LocalDate(today.getYear(), today.getMonthOfYear(), 1);
              adjDate = adjDate.plusMonths(1);
            } else if (dayOfMonth < 15) {
              adjDate = new LocalDate(today.getYear(), today.getMonthOfYear(), 15);
            }
        }
        
        terms.setStartDate(today);
        terms.setAdjustmentDate(adjDate);
        
        Iterator<ScheduledPayment> result = AmortizationCalculator.getPayments(terms);
        
        int resultCount = 0;
        Money interestTotal = new Money("0.00");
        while(result.hasNext()) {
            resultCount++;
            ScheduledPayment payment = result.next();
            interestTotal = interestTotal.add(payment.getInterest());
        }
        assertEquals("Interest Only payment count", 12, resultCount);
        assertEquals("Interest Only interest total", new Money("12"), interestTotal);

    }

    
    @Test
    public void testGetPaymentsInterestOnlyExtraPrincipal() {
        System.out.println("getPaymentsInterestOnlyExtraPrincipal");
        
        Money amount = new Money("100.00");
        double rate = 12.0;

        AmortizationAttributes terms = new AmortizationAttributes();
        terms.setInterestOnly(true);
        terms.setLoanAmount(amount);
        terms.setTermInMonths(12);
        terms.setInterestRate(rate);
        
        double interestOnlyMonthlyPayment = AmortizationCalculator.getInterestOnlyMonthlyPayment(amount.getAmount().doubleValue(), rate);
        Money installment = new Money(BigDecimal.valueOf(interestOnlyMonthlyPayment));
        installment = installment.add(new Money("15.00"));
        terms.setRegularPayment(installment);
        
        LocalDate today = new LocalDate();
        LocalDate adjDate = new LocalDate();
        
        int dayOfMonth = today.getDayOfMonth();
        if (1 != dayOfMonth) {
            if (dayOfMonth > 15) {
              adjDate = new LocalDate(today.getYear(), today.getMonthOfYear(), 1);
              adjDate = adjDate.plusMonths(1);
            } else if (dayOfMonth < 15) {
              adjDate = new LocalDate(today.getYear(), today.getMonthOfYear(), 15);
            }
        }
        
        terms.setStartDate(today);
        terms.setAdjustmentDate(adjDate);
        
        Iterator<ScheduledPayment> result = AmortizationCalculator.getPayments(terms);
        
        int resultCount = 0;
        Money interestTotal = new Money("0.00");
        while(result.hasNext()) {
            resultCount++;
            ScheduledPayment payment = result.next();
            interestTotal = interestTotal.add(payment.getInterest());
        }
        assertEquals("Interest Only payment count -extra principal", 7, resultCount);
        assertEquals("Interest Only interest total", new Money("7"), interestTotal);

    }

    
    @Test
    public void testGetPaymentsAmortized() {
        System.out.println("testGetPaymentsAmortized");
        
        Money amount = new Money("200000.00", Currency.getInstance("CAD"), RoundingMode.HALF_UP);
        double rate = 8.0;
        int termInMonths = 36;
        
        AmortizationAttributes terms = new AmortizationAttributes();
        terms.setInterestOnly(false);
        terms.setLoanAmount(amount);
        terms.setTermInMonths(termInMonths);
        terms.setAmortizationPeriodMonths(20 * 12);
        terms.setCompoundingPeriodsPerYear(2);
        terms.setInterestRate(rate);
        
        double amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(amount, rate, 2, terms.getAmortizationPeriodMonths());
        Money regularPayment = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), amount.getCurrency(), amount.getRoundingMode());
        terms.setRegularPayment(regularPayment);
        
        LocalDate today = new LocalDate();
        LocalDate adjDate = new LocalDate();
        
        int dayOfMonth = today.getDayOfMonth();
        if (1 != dayOfMonth) {
            if (dayOfMonth > 15) {
              adjDate = new LocalDate(today.getYear(), today.getMonthOfYear(), 1);
              adjDate = adjDate.plusMonths(1);
            } else if (dayOfMonth < 15) {
              adjDate = new LocalDate(today.getYear(), today.getMonthOfYear(), 15);
            }
        }
        
        terms.setStartDate(today);
        terms.setAdjustmentDate(adjDate);
        
        Iterator<ScheduledPayment> result = AmortizationCalculator.getPayments(terms);
        
        int resultCount = 0;
        Money interestTotal = new Money("0.00");
        while(result.hasNext()) {
            resultCount++;
            ScheduledPayment payment = result.next();
            interestTotal = interestTotal.add(payment.getInterest());
            System.out.println("" + payment);
        }
        assertEquals("Amortized payment count", termInMonths, resultCount);
        assertEquals("Amortized Interest total", new Money("45681.38"), interestTotal);

    }

    
    /**
     * Test of getInterestOnlyMonthlyPayment method, of class AmortizationCalculator.
     */
    @Test
    public void testGetInterestOnlyMonthlyPayment() {
        System.out.println("getInterestOnlyMonthlyPayment");
        Money amount = new Money("100000.00");
        double rate = 12.0;
        Money expResult = new Money("1000.00");
        
        double interestOnlyMonthlyPayment = AmortizationCalculator.getInterestOnlyMonthlyPayment(amount.getAmount().doubleValue(), rate);
        Money result = new Money(BigDecimal.valueOf(interestOnlyMonthlyPayment));
        
        assertEquals(expResult, result);
    }

    /**
     * Test of getAmortizedMonthlyPayment method, of class AmortizationCalculator.
     */
    @Test
    public void testGetAmortizedMonthlyPayment() {
        System.out.println("getAmortizedMonthlyPayment");
        Money loanAmount = new Money("100000.00", Currency.getInstance("CAD"), RoundingMode.CEILING);
        double i = 12.0;
        int compoundPeriod = 2;
        int amortizationPeriod = 25 * 12;
        Money expResult = new Money("1031.90");
        
        double amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(loanAmount, i, compoundPeriod, amortizationPeriod);
        Money result = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), loanAmount.getCurrency(), loanAmount.getRoundingMode());
        
        assertEquals("Semi-annual compounding period", expResult, result);
        
        compoundPeriod = 12;
        loanAmount = new Money("100000.00", Currency.getInstance("CAD"), RoundingMode.HALF_UP);
        expResult = new Money("1053.22");
        
        amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(loanAmount, i, compoundPeriod, amortizationPeriod);
        result = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), loanAmount.getCurrency(), loanAmount.getRoundingMode());
        
        assertEquals("Monthly compound period", expResult, result);

        compoundPeriod = 12;
        loanAmount = new Money("100000.00", Currency.getInstance("CAD"), RoundingMode.CEILING);
        expResult = new Money("1053.23");
        
        amortizedMonthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(loanAmount, i, compoundPeriod, amortizationPeriod);
        result = new Money(BigDecimal.valueOf(amortizedMonthlyPayment), loanAmount.getCurrency(), loanAmount.getRoundingMode());
        
        assertEquals("Monthly compound period, ceiling", expResult, result);

    }
    
}
