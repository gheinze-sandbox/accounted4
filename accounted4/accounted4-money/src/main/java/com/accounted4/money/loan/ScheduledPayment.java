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
 * A structure to hold the information for a payment which can represent a line
 * item in an amortization schedule
 * 
 * @author Glenn Heinze 
 */
@Data
public class ScheduledPayment {

    private int paymentNumber;
    private LocalDate paymentDate;
    private Money interest;
    private Money principal;
    private Money balance;


    public Money getPayment() {
        return getInterest().add( getPrincipal() );
    }
    
    
}
