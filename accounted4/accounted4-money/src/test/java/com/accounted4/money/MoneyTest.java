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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Exercise Money constructors and apis to verify contracts.
 * 
 * @author Glenn Heinze 
 */
public class MoneyTest {
    

    /**
     * Don't allow creation of Money without an amount
     */
    @Test(expected=IllegalArgumentException.class)
    public void testBigDecimalConstructorNullAmount() {
        BigDecimal amount = null;
        new Money(amount, Currency.getInstance("USD"), RoundingMode.CEILING);
        fail("Money amount may not be null");
    }
    
    
    /**
     * Don't allow creation of Money without a currency
     */
    @Test(expected=IllegalArgumentException.class)
    public void testBigDecimalConstructorNullCurrency() {
        new Money(new BigDecimal("1.23"), null, RoundingMode.CEILING);
        fail("Currency may not be null");
    }
    
    
    /**
     * Don't allow creation of Money without a rounding mode
     */
    @Test(expected=IllegalArgumentException.class)
    public void testBigDecimalConstructorNullRoundingMode() {
        new Money(new BigDecimal("1.23"), Currency.getInstance("USD"), null);
        fail("RoundingMode may not be null");
    }
    
    
    /**
     * Currencies have a major an minor amount (ex dollars and cents).
     * Money does not support fractional minor amounts.
     */
    @Test
    public void testBigDecimalConstructorNarrowingAmount() {
        
        // USD allows for two decimal places.  If there are more, use ceiling
        Money money = new Money(new BigDecimal("1.231"), Currency.getInstance("USD"), RoundingMode.CEILING);
        BigDecimal expected = new BigDecimal("1.24");
        BigDecimal actual = money.getAmount();
        assertEquals("Fractions beyond that supported by the currency will be removed according to the rounding mode", expected, actual);
        
        // JPY allows for 0 decimal places.  If there are more, use ceiling
        money = new Money(new BigDecimal("123.4"), Currency.getInstance("JPY"), RoundingMode.CEILING);
        expected = new BigDecimal("124");
        actual = money.getAmount();
        assertEquals("Fractions beyond that supported by the currency will be removed according to the rounding mode", expected, actual);
        
    }
    
    
    /**
     * Monetary amounts can be created using a system default rounding mode
     */
    @Test
    public void testBigDecimalConstructorDefaultRounding() {
        Money m = new Money(new BigDecimal("1.231"), Currency.getInstance("USD"));
        assertEquals("Default rounding mode", m.getRoundingMode(), AppConfig.getInstance().getDefaultRoundingMode());
    }

    
    /**
     * Monetary amounts can be created using a system default currency
     */
    @Test
    public void testBigDecimalConstructorDefaultCurrency() {
        Money m = new Money(new BigDecimal("1.231"));
        assertEquals("Default currency", m.getCurrency(), AppConfig.getInstance().getDefaultCurrency());
    }
    
    
    /**
     * A Money object can be created from another Money object
     */
    @Test
    public void testMoneyConstructor() {
        Money money = new Money(new BigDecimal("1.231"));
        Money actual = new Money(money);
        assertEquals("A Money object created from another Money object should be equivalent", money, actual);
        assertFalse("A Money object created from another Money object should be a different object", money == actual);
    }
    
    
    /**
     * A Money object was indeed created as expected.
     */
    @Test
    public void testMoneyConstructorSanity() {
        
        BigDecimal expectedAmount = new BigDecimal("1.23");
        Currency expectedCurrency = Currency.getInstance("USD");
        RoundingMode expectedRoundingMode = RoundingMode.CEILING;
        
        Money actual = new Money(expectedAmount, expectedCurrency, expectedRoundingMode);
        assertEquals("Creation amount", expectedAmount, actual.getAmount());
        assertEquals("Creation currency", expectedCurrency, actual.getCurrency());
        assertEquals("Creation rounding mode", expectedRoundingMode, actual.getRoundingMode());
        
    }
    

    /**
     * Check if two different Moneys share a currency
     */
    @Test
    public void testIsSameCurrency() {
        
        Money moneyUsd = new Money(new BigDecimal("1.23"), Currency.getInstance("USD"));
        Money moneyUsd2 = new Money(new BigDecimal("1.23"), Currency.getInstance("USD"));
        Money moneyCad = new Money(new BigDecimal("1.23"), Currency.getInstance("CAD"));

        assertTrue("Currencies same", moneyUsd.isSameCurrency(moneyUsd2));
        assertTrue("Currencies same symmetric", moneyUsd2.isSameCurrency(moneyUsd));
        assertFalse("Currencies different", moneyUsd.isSameCurrency(moneyCad));
        
    }

    
    /**
     * Addition: amount is correct, base currency and rounding mode is applied
     * currency mismatch throws exception
     */
    @Test(expected=CurrencyMismatchRuntimeException.class)
    public void testAdd() {
        
        BigDecimal magnitude1 = new BigDecimal("1.11");
        BigDecimal magnitude2 = new BigDecimal("2.22");
        BigDecimal magnitude3 = new BigDecimal("3.33");
        BigDecimal magnitude4 = new BigDecimal("-1.11");
        
        Currency usd = Currency.getInstance("USD");
        Currency cad = Currency.getInstance("CAD");
        
        Money moneyUsd = new Money(magnitude1, usd, RoundingMode.CEILING);
        Money moneyUsd2 = new Money(magnitude2, usd, RoundingMode.FLOOR);

        Money sum = moneyUsd.add(moneyUsd2);
        assertTrue("Addition result has same currency", sum.getCurrency().equals(moneyUsd.getCurrency()));
        assertTrue("Addition result has base rounding mode", sum.getRoundingMode().equals(moneyUsd.getRoundingMode()));
        assertTrue("Amounts add up", sum.getAmount().equals( magnitude1.add(magnitude2)));
        
        Money moneyCad = new Money(magnitude3, cad);
        Money moneyCadNeg = new Money(magnitude4, cad);
        Money moneyCadExp = new Money(magnitude2, cad);
        
        sum = moneyCad.add(moneyCadNeg);
        assertTrue("Addition of negative amount", sum.equals(moneyCadExp));
        
        // Different currencies should throw an exception
        sum = moneyUsd.add(moneyCad);
        
        fail("Addition: different currencies should throw an exception");
        
    }

    
    /**
     * Subtraction: amount is correct, base currency and rounding mode is applied
     * currency mismatch throws exception
     */
    @Test
    public void testSubtract() {
        
        BigDecimal magnitude111 = new BigDecimal("1.11");
        BigDecimal magnitude222 = new BigDecimal("2.22");
        BigDecimal magnitude333 = new BigDecimal("3.33");
        
        Currency usd = Currency.getInstance("USD");
        Currency cad = Currency.getInstance("CAD");
        
        Money moneyUsd111 = new Money(magnitude111, usd, RoundingMode.CEILING);
        Money moneyUsd222 = new Money(magnitude222, usd, RoundingMode.FLOOR);
        Money moneyCad333 = new Money(magnitude333, cad);

        Money sum = moneyUsd111.subtract(moneyUsd222);
        assertTrue("Subtraction result has same currency", sum.getCurrency().equals(moneyUsd111.getCurrency()));
        assertTrue("Subtraction result has base rounding mode", sum.getRoundingMode().equals(moneyUsd111.getRoundingMode()));
        assertTrue("Subtraction difference is as expected", sum.getAmount().equals( magnitude111.subtract(magnitude222)));

        BigDecimal magnitudeN111 = new BigDecimal("-1.11");
        Money moneyCadN111 = new Money(magnitudeN111, cad);
        sum = moneyCad333.subtract(moneyCadN111);
        Money expected = new Money("4.44", cad);
        assertTrue("Subtracting a negative amount", sum.equals(expected));
        
        // Different currencies should throw an exception
        try {
            sum = moneyUsd111.subtract(moneyCad333);
            fail("Subtraction: different currencies should throw an exception");
        } catch(CurrencyMismatchRuntimeException cmm) {
        }
        
    }


    /**
     * Multiplication
     */
    @Test
    public void testMultiply() {

        // Use non-defaults
        Money money = new Money(new BigDecimal("2.22"), Currency.getInstance("USD"), RoundingMode.CEILING);
        Money product = money.multiply(-2.5d);
        Money expected = new Money(new BigDecimal("-5.55"), Currency.getInstance("USD"), RoundingMode.CEILING);
        
        assertTrue("Multiplication", product.equals(expected));
        
    }

    /**
     * Division.  50.00/6 = 8.33
     */
    @Test
    public void testDivide() {

        Currency usd = Currency.getInstance("USD");

        Money money = new Money(new BigDecimal("50.00"), usd, RoundingMode.CEILING);
        Money quotient = money.divide(new BigDecimal("6"));
        Money expected = new Money(new BigDecimal("8.34"), usd, RoundingMode.CEILING);
        
        assertEquals("Division CEILING", expected, quotient);
        
        money = new Money(new BigDecimal("50.00"), usd, RoundingMode.FLOOR);
        quotient = money.divide(new BigDecimal("6"));
        expected = new Money(new BigDecimal("8.33"), usd, RoundingMode.FLOOR);
        
        assertTrue("Division FLOOR", quotient.equals(expected));
        
    }
    
    
    /**
     * A split into 0 partitions should throw an exception
     */
    @Test(expected=ArithmeticException.class)
    public void testSplitOnZero() {
        Money money = new Money(new BigDecimal("50.00"));
        money.split(0);
        fail("Can't split by 0");
    }

    /**
     * A split into negative partitions should throw an exception
     */
    @Test(expected=ArithmeticException.class)
    public void testSplitOnNegative() {
        Money money = new Money(new BigDecimal("50.00"));
        money.split(-6);
        fail("Can't split by negative number");
    }

    /**
     * Splitting into n should result in n containers.
    */
    @Test
    public void testSplit() {
        Money money = new Money(new BigDecimal("50.00"));
        Split split = money.split(6);
        assertEquals("Split into expected number of containers", 6, split.getPartitions(DivideType.Equalized).size());
    }
    
    
    /**
     * Can only compare similar currencies, then based on amount
     */
    @Test
    public void testCompareTo() {
        
        BigDecimal magnitude1 = new BigDecimal("1.11");
        BigDecimal magnitude2 = new BigDecimal("2.22");
        BigDecimal magnitude3 = new BigDecimal("3.33");
        
        Currency usd = Currency.getInstance("USD");
        Currency cad = Currency.getInstance("CAD");
        
        Money moneyUsd = new Money(magnitude1, usd, RoundingMode.CEILING);
        Money moneyUsd2 = new Money(magnitude2, usd, RoundingMode.FLOOR);
        Money moneyCad = new Money(magnitude3, cad);
        Money moneyCad2 = new Money(magnitude3, cad);

        try {
            moneyUsd.compareTo(moneyCad);
            fail("Comparisons between Money objects with different currencies should fail");
        } catch(CurrencyMismatchRuntimeException cmm) {
        }
        
        assertTrue("Less than", moneyUsd.compareTo(moneyUsd2) < 0);
        assertTrue("Greater than", moneyUsd2.compareTo(moneyUsd) > 0);
        assertTrue("Equal", 0 == moneyCad.compareTo(moneyCad2));

        assertTrue("Less than 2", moneyUsd.lessThan(moneyUsd2));
        assertFalse("Less than 2b", moneyUsd2.lessThan(moneyUsd));

        assertTrue("Greater than 2", moneyUsd2.greaterThan(moneyUsd));
        assertFalse("Greater than 2b", moneyUsd.greaterThan(moneyUsd2));

        assertTrue("Equal 2", moneyCad.equals(moneyCad2));

        assertFalse("Equality class mis-match", moneyCad.equals(BigDecimal.ONE));
    }

    @Test
    public void testEquality() {
        
        Money money1 = new Money("1.23", Currency.getInstance("USD"), RoundingMode.FLOOR);
        assertFalse("Null mismatch", money1.equals(null));
        
        //Money money2 = new Money("1.23", Currency.getInstance("USD"), RoundingMode.CEILING);
        //assertFalse("Rounding mode mismatch", money1.equals(money2));
        
        Money money2 = new Money("1.23", Currency.getInstance("CAD"), RoundingMode.FLOOR);
        assertFalse("Currency mismatch", money1.equals(money2));
        
        money2 = new Money("1.24", Currency.getInstance("USD"), RoundingMode.FLOOR);
        assertFalse("Magnitude mismatch", money1.equals(money2));
        
    }

    @Test
    public void testToString() {
        
        Money money1 = new Money("1.23", Currency.getInstance("USD"), RoundingMode.FLOOR);
        assertEquals("Equals without currency", "1.23", money1.toString());
        assertEquals("Equals with currency", "USD 1.23", money1.toStringWithCurrency());
        assertEquals("Equals with debug", "USD 1.23[FLOOR]", money1.toDebugString());
    }
}
