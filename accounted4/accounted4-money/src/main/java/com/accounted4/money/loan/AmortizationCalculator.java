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



/**
 * Utility functions for amortization schedule generation
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class AmortizationCalculator {
    

    /**
     * Generate an ordered list of payments forming an amortization schedule.
     *
     * If the payment is greater than the regular calculated amortization payment,
     * then the monthly surplus is used as extra principal payment.
     * 
     * If the payment is less than the regular monthly amortization payments,
     * then the supplied payment is ignored and a regular schedule is generated.
     *
     * @param terms
     * 
     * @result An ordered list of payments which comprise the set of regular
     * payments fulfilling the terms of the given amortization parameters.
     */
    public static Iterator<ScheduledPayment> getPayments(AmortizationAttributes terms) {
        
        return (terms.isInterestOnly()) ?
                new InterestOnlyIterator(terms) :
                new AmortizedIterator(terms);
        
    }
         
    
    
    /* ===============================
     * Abstract class holding supplied and calculated values required for calculating
     * subsequent payments, whethere interest only or amortized.
     */
    private static abstract class AmortizationIterator implements Iterator<ScheduledPayment> {

        protected final Money zeroMoney;  // A zero Money amount with the same currency and rounding mode as the loan amount
        
        protected final AmortizationAttributes terms;
        
        // Short-cuts to "term" attributes
        protected final Money regularPayment;
        protected final Currency currency;
        protected final RoundingMode roundingMode;
        
        // TODO: this only considers monthly payments, not, say, twice a month
        
        /*
         * The calculatedMonthlyPayment is the computed monthly payment. It could be less
         * then the specified monthly payment if there is a desire to pay extra principal
         * during each payment period. It is also maintained as a double (fractional values)
         * to minimize the drift which would start occurring with repeated rounding.
         * 
         * calculatedMonthlyPaymentMoney holds the "rounded" version used for display
         */
        protected final double calculatedMonthlyPayment; // minimum that needs to be paid monthly
        protected final Money calculatedMonthlyPaymentMoney;
        
        // Numerical methods: can't necessarily compare a balance amount to zero because of
        // representation issues, so truncate after a certain number of digits pre-comparison
        protected final double truncationFactor;
        
        protected int paymentNumber = 0;  // Counter of payments already calculated
        
        // Maintained as a double to minimize the drift which would start occurring with repeated rounding
        protected double balance;

        
        public AmortizationIterator(AmortizationAttributes terms) {
            
            this.terms = terms;
            
            // Set short-cuts
            this.regularPayment = terms.getRegularPayment();
            this.currency = terms.getLoanAmount().getCurrency();
            this.roundingMode = terms.getLoanAmount().getRoundingMode();
            
            // Initialize balance to loan amount
            this.balance = terms.getLoanAmount().getAmount().doubleValue();

            calculatedMonthlyPayment = terms.isInterestOnly() ?
                    
                    getInterestOnlyMonthlyPayment(terms.getLoanAmount().getAmount().doubleValue(), terms.getInterestRate()) :
                    
                    getAmortizedMonthlyPayment(
                        terms.getLoanAmount(),
                        terms.getInterestRate(),
                        terms.getCompoundingPeriodsPerYear(),
                        terms.getAmortizationPeriodMonths() );
            
            calculatedMonthlyPaymentMoney = new Money(BigDecimal.valueOf(calculatedMonthlyPayment), currency, roundingMode);
                    
            // The regular payment has to be at least the calculated payment, if not more
            assert !calculatedMonthlyPaymentMoney.greaterThan(regularPayment);
            
            // Two decimal places, truncate after 5 digits
            truncationFactor = Math.pow(10.0, currency.getDefaultFractionDigits() * 2 + 1);
            
            zeroMoney = new Money("0", currency, roundingMode);
            
        }

        
        /**
         * True if there is still a balance to be paid and loan term has not yet been reached
         * 
         * @return 
         */
        @Override
        public boolean hasNext() {
            return ( 
                    paymentNumber < terms.getTermInMonths() &&
                    Math.round(balance * truncationFactor ) > 0L
                    );
        }

        
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator remove operation not supported.");
        }
        
    }

    
    
    
    /* =============================== */
    
    /**
     * Iterator for an interest-only payment schedule. If over payments are
     * specified, principal will be reduced
     * 
     */
    public static class InterestOnlyIterator extends AmortizationIterator {

        private final Money extraPrincipal;
        
        public InterestOnlyIterator(AmortizationAttributes terms) {
            super(terms);
            if ( regularPayment.greaterThan(calculatedMonthlyPaymentMoney) ) {
                extraPrincipal = regularPayment.subtract(calculatedMonthlyPaymentMoney);
            } else {
                extraPrincipal = zeroMoney;
            }
        }

        
        @Override
        public ScheduledPayment next() {
            
            paymentNumber++;
            
            LocalDate date = terms.getAdjustmentDate().plusMonths(paymentNumber);

            ScheduledPayment payment = new ScheduledPayment();
            payment.setPaymentNumber(paymentNumber);
            payment.setPaymentDate(date);
            payment.setInterest(calculatedMonthlyPaymentMoney);

            // Normally, the regular payment is pure interest and it should be the same as the calculated payment
            if (extraPrincipal.equals(zeroMoney)) {
                payment.setPrincipal(zeroMoney);
                payment.setBalance(terms.getLoanAmount());
                return payment;
            }
            
            // An overpayment of interest needs to reduce the balance
            
            Money balanceMoney = new Money(BigDecimal.valueOf(balance), currency, roundingMode);

            if (extraPrincipal.greaterThan(balanceMoney)) {
                // The loan is being completely paid out. Just pay the remaining
                // principal of the loan, and not the surplas principal being.
                payment.setPrincipal(balanceMoney);
                balance = 0.0;
            } else {
                // Pay down the loan with the extra principal supplied
                payment.setPrincipal(extraPrincipal);
                balance = balance - extraPrincipal.getAmount().doubleValue();
            }

            // Since the balance has been adjusted, recompute
            balanceMoney = new Money(BigDecimal.valueOf(balance), currency, roundingMode);
            
            payment.setBalance(balanceMoney);
            
            // System.out.println("IO: " + payment);
            return payment;
            
        }
        
    }
    
    
    /* =============================== */
    
    public static class AmortizedIterator extends AmortizationIterator {         
        
        private final double j;   // period rate: ie rate until compounding trigger (rate for 6 months for semi-annual, rate for 1 month for monthly)
        private final double overpayment;
        private double thePayment;
        private Money thePaymentMoney;
        
        public AmortizedIterator(AmortizationAttributes terms) {
            super(terms);
            
            assert(terms.getLoanAmount().greaterThan(zeroMoney));
            assert(terms.getTermInMonths() > 0);
            assert(terms.getAmortizationPeriodMonths() > 0);
            assert(terms.getCompoundingPeriodsPerYear() > 0);
            assert(terms.getInterestRate() > 0.0d);            
            
            j = getPeriodRate(terms.getInterestRate(), terms.getCompoundingPeriodsPerYear());

            thePayment = regularPayment.getAmount().doubleValue();
            if ( Math.round( (thePayment - calculatedMonthlyPayment) * truncationFactor ) <= 0L ) {
                // The payment has to be at least as much as the calculated monthly payment
                thePayment = calculatedMonthlyPayment;
            }
            
            overpayment = thePayment - calculatedMonthlyPayment;
            thePaymentMoney = new Money( BigDecimal.valueOf(thePayment), currency, roundingMode);
            
        }
        
        
        @Override
        public ScheduledPayment next() {
            
            paymentNumber++;

            LocalDate date = terms.getAdjustmentDate().plusMonths(paymentNumber);

            double computedInterest = balance * j;
            Money interest = new Money( BigDecimal.valueOf(computedInterest), currency, roundingMode);
            
            double principal = thePayment - computedInterest + overpayment;
            
            balance -= principal;
            Money balanceMoney = new Money(BigDecimal.valueOf(balance), currency, roundingMode);
            
            Money principalMoney = thePaymentMoney.subtract(interest);
            
            ScheduledPayment payment = new ScheduledPayment();
            payment.setPaymentNumber(paymentNumber);
            payment.setPaymentDate(date);
            payment.setInterest(interest);
            payment.setPrincipal(principalMoney);
            payment.setBalance(balanceMoney);
            
            return payment;
            
        }

    }
    
    
    public static Money getMonthlyPayment(AmortizationAttributes amAttrs) {

        double monthlyPayment;
        
        if (amAttrs.isInterestOnly()) {
            monthlyPayment = AmortizationCalculator.getInterestOnlyMonthlyPayment(amAttrs.getLoanAmount().getAmount().doubleValue(), amAttrs.getInterestRate());
        } else {
            monthlyPayment = AmortizationCalculator.getAmortizedMonthlyPayment(
                    amAttrs.getLoanAmount(),
                    amAttrs.getInterestRate(),
                    amAttrs.getCompoundingPeriodsPerYear(),
                    amAttrs.getAmortizationPeriodMonths()
                    );
        }
        
        return new Money(BigDecimal.valueOf(monthlyPayment));

    }

    
    /**
     * Given an amount and an annual interest rate, return the monthly payment
     * for an interest only loan.
     *
     * @param amount the principal amount
     * @param rate the annual interest rate expressed as a percent
     */
    public static double getInterestOnlyMonthlyPayment(double amount, double rate) {
        // percent to decimal, annual rate to period (monthly) rate
        return amount * rate / 100. / 12.; 
    }
         

    
    /**
     * Given an amount and an annual interest rate, return the monthly payment
     * for an interest only loan.
     *
     * @param a the principal
     * @param i the interest rate expressed as a percent
     * @param compoundPeriodsPerYear  The number of times a year interest is calculated
     *     Canadian law specifies semi-annually (ie 2x a year).  Americans
     *     typically use monthly (ie 12x a year)
     * @param amortizationPeriod  The number of months the loan is spread over
     *
     * @return The expected monthly payment amortized over the given period.
     */
    public static double getAmortizedMonthlyPayment(
            Money  loanAmount,
            double i,
            int    compoundPeriodsPerYear,
            int    amortizationPeriod ) {
        
        double a = loanAmount.getAmount().doubleValue();
        
        // periodRate
        double j = getPeriodRate(i, compoundPeriodsPerYear); 
                //Math.pow( (1 + i/(compoundPeriodsPerYear*100.0)), (compoundPeriodsPerYear/12.0) ) - 1;
        // double j = Math.pow( (1 + i/200.0), (1.0/6.0) ); // Canadian simplified
        
        // periods per year (ie monthly payments)
        int n = 12;
        
        // amortization period in years
        double y = amortizationPeriod/12.0;
                
        double monthlyPayment = a*(j)/(1.0-Math.pow(j+1.0,-n*y));
        
        return monthlyPayment;
    }

    
    /**
     * Retrieve the interest rate for the compounding period based on the annual interest rate.
     * 
     * @param annualInterestRatePercent input annual interest rate as a percent (ie 8.25 for 8.25%)
     * @param compoundPeriodsPerYear 2 if compounding semi-annually, 12 if compounding monthly
     * @return interest rate as a decimal (ie .125 for 12.5%)
     */
    public static double getPeriodRate(double annualInterestRatePercent, int compoundPeriodsPerYear) {
        return Math.pow( (1 + annualInterestRatePercent/(compoundPeriodsPerYear*100.0)), (compoundPeriodsPerYear/12.0) ) - 1;
    }
    
    
}
