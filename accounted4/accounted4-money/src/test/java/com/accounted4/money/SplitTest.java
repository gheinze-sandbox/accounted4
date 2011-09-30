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


import java.util.Collection;
import java.util.Currency;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class SplitTest {
    
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

    }



}
