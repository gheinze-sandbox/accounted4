/*
 * Copyright 2011 Glenn Heinze <glenn@gheinze.com>.
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
package com.accounted4.money;

/**
 * Determines the handling of fractional units when monetary amounts are divided.
 *
 * For example, $50.00/6 = $8.333...
 *
 * But if we wish to present a collection of "equal" amounts, which when summed
 * together equal the original total, there are several ways the fractional 
 * units can be distributed. Three example distributions are shown below.
 * 
 * A: represents a straight divide with normal rounding rules.  The sum does
 * not tally to the original total because of the lost fractions. This divide
 * type is not supported.
 *
 * B: represents an "equalized" divide where no amount differs by more than
 * a "penny". All extra "pennies" are distributed among the early entries.
 *
 * C: represents an "adjusted" divide: the division is always rounded up
 * and the final amount is adjusted down to compensate for the additional
 * fractional pennies. An example usage of this case would be for building an
 * amortization schedule: we would not wish to tell the mortgagor to make
 * the first two payments in one amount (8.33) and the remaining in another
 * amount (8.34) as in case B. Rather, we would request all payments to be
 * of one amount (8.34) with an adjustment made for the final amount (8.30).
 * The rounding should always be up since it would not be desirable for the
 * mortgagor to be behind in payments at any one time.
 *
 *      A       B       C
 * 1    8.33    8.34    8.34
 * 2    8.33    8.34    8.34
 * 3    8.33    8.33    8.34
 * 4    8.33    8.33    8.34
 * 5    8.33    8.33    8.34
 * 6    8.33    8.33    8.30
 *      ====    ====    ====
 *     49.98   50.00   50.00
 *
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public enum DivideType {

    /**
     * An "equalized" divide where no amount differs by more than a penny.
     * All extra pennies are distributed among the early entries. "B"
     */
    Equalized,
    
    /**
     * An "adjusted" divide: the division is always rounded up and the final
     * amount is adjusted down to compensate for the fractional pennies
     * (usage: amortization schedule).  "C"
     */
    Final_Adjustment;
    
}
