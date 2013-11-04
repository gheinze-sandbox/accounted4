/*
 * Copyright 2011 Glenn Heinze .
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


import java.util.Currency;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;


/**
 *
 * @author Glenn Heinze 
 */
public class SplitTest {
    
    @Test
    public void testArgumentValidity() {
     
        try {
            new Split(null, 6);
            fail("Money must be provided as an argument to Split");
        } catch(IllegalArgumentException ex) {
        }
        
        try {
            new Split(new Money("10.00"), 0);
            fail("Cannot split money into 0 partitions");
        } catch(IllegalArgumentException ex) {
        }
        
        try {
            new Split(new Money("10.00"), -1);
            fail("Cannot split money into negative number of partitions");
        } catch(IllegalArgumentException ex) {
        }
        
    }
    
    /**
     * Test of getFloor method, of class Split.
     */
    @Test
    public void testBasicRun() {
        
        Currency usd = Currency.getInstance("USD");
        Money money = new Money("50.00", usd);
        Split split = new Split(money, 6);

        Money floor = new Money("8.33", usd);
        Money ceiling = new Money("8.34", usd);
        
        assertEquals("Floor", split.getFloor(), floor);
        assertEquals("Ceiling", split.getCeiling(), ceiling);
        assertEquals("Remainder", split.getRemainder(), new Money("0.02", usd));
        assertEquals("Remainder units", split.getRemainderUnitCount(), 2);

        int count = 0;
        for (Money part : split.getPartitions(DivideType.Equalized)) {
            if (count++ < 2) {
                assertEquals("Part", part, ceiling);
            } else {
                assertEquals("Part", part, floor);
            }
        }
        assertEquals("Partition size", 6, count);

        count = 0;
        Money total = new Money("0", usd);
        for (Money part : split.getPartitions(DivideType.Final_Adjustment)) {
            if (count++ < 5) {
                assertEquals("Part", part, ceiling);
                total = total.add(ceiling);
            } else {
                assertEquals("Final payment should be difference between amount and that already paid", part, money.subtract(total));
            }
        }

    }


    @Test
    public void testEvenDivide() {
        
        Currency jpy = Currency.getInstance("JPY");
        Money money = new Money("50", jpy);
        Split split = new Split(money, 5);

        Money floor = new Money("10", jpy);
        Money ceiling = new Money("10", jpy);
        
        assertEquals("Floor", split.getFloor(), floor);
        assertEquals("Ceiling", split.getCeiling(), ceiling);
        assertEquals("Remainder", split.getRemainder(), new Money("0", jpy));
        assertEquals("Remainder units", split.getRemainderUnitCount(), 0);

        int count = 0;
        for (Money part : split.getPartitions(DivideType.Equalized)) {
            assertEquals("Part", part, ceiling);
            count++;
        }
        assertEquals("Partition size", 5, count);

        count = 0;
        Money total = new Money("0", jpy);
        for (Money part : split.getPartitions(DivideType.Final_Adjustment)) {
            assertEquals("Part", part, ceiling);
            total = total.add(ceiling);
            count++;
        }
        assertEquals("Parts total back to the original amount", money, total);

    }


}
