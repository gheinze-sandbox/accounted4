/*
 * Copyright 2012 Glenn Heinze .
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
import lombok.Data;
import org.joda.time.LocalDate;

/**
 * Bean to hold the properties required to compute amortized payments.
 * 
 * @author Glenn Heinze 
 */
@Data
public class AmortizationAttributes {


    private Money loanAmount;         // original principal amount   
    private Money regularPayment;     // monthly payment to be made, assumed monthly
    private LocalDate startDate;      // loan start date
    private LocalDate adjustmentDate; // date from which amortization calculations commence
    private int termInMonths;         // number of months from the adjustment date at which amortization stops and remaining principal is due
    private boolean interestOnly;     // true if this is an interest only calculation (ie no amortization)
    private int amortizationPeriodMonths; // number of months over which to amortize the payments. If payments are made till this date, principal remaining will be 0
    private int compoundingPeriodsPerYear;  // number of times a year interest compounding is calculated. Canadian rules: 2 (semi-annually). American rules: 12 (monthly)
    private double interestRate;       // the nominal interest rate being paid (effective rate can be higher if compounding)

}
