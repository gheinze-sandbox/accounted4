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


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;


/**
 * Represents an immutable monetary value.
 *
 * <p>The <tt>Money</tt> class tries to address currency and rounding issues
 * by encapsulating the amount as a <tt>BigDecimal</tt> and associating an
 * ISO currency to determine fractional units..
 *
 * <p>Partially based on Martin Fowler's design patterns for Quantity and Money.
 *
 * Money is expressed as a quantity of a currency.
 *
 * Each currency has a standard number of accepted fractional units.
 * For example, Canadian dollars have 2 fractional units whereas the
 * Japanese yen has 0 fractional units.
 * 
 * http://en.wikipedia.org/wiki/ISO_4217#cite_note-ReferenceA-0
 * 
 * Currency code, decimal places
 * CAD 2
 * JPY 0
 * TND 3
 * 
 * Often currencies are expressed as integer units and then normalized for
 * display with the appropriate number of fractional units. However,
 * since BigDecimal uses internal integer representation and provides a
 * rounding mechanism, Money uses BigDecimal to represent the quantity
 * instead.
 * 
 * Note that fractional Monetary amounts (ie fractional amounts that are 
 * beyond that supported by the currency) are not supported. For example,
 * US $1.253 would be stored as US $1.25 or US $1.26 depending on the
 * rounding mode specified, but the fraction of a cent will be lost.
 * 
 * The contract for all Money operations is that they must only be invoked
 * on Moneys of the same currency. Performing an operation on Moneys with
 * two different currencies (say an an addition of a USD money amount and
 * a CAD money amount) will result in a CurrencyMismatchRuntimeException.
 * 
 * @author Glenn Heinze 
 */
public final class Money implements Serializable, Comparable<Money> {

    
    private static final long serialVersionUID = 1L;
    // TODO: Externalizable
    
    /**
     * Monetary amounts defined without a currency will be assigned the
     * default currency.
     */
    private static final AppConfig APP_CONFIG = AppConfig.getInstance();
    
    /**
     * The amount with fractional units. In the case of CAD (Canadian dollars),
     * Currency.getInstance("CAD").getDefaultFractionDigits()
     * returns a value of 2, so the a value may have 2 decimal places (1.00)
     */
    private final BigDecimal amount;

    /**
     * The ISO currency code of the monetary unit.
     */
    private final Currency currency;

    /**
     * How fractional amounts should be dealt with, typically applicable in
     * division operations.
     */
    private final RoundingMode roundingMode;





    /*-------------------------------
     * Constructors
     *-------------------------------
     */


    // -------------- BigDecimal Constructors ---------------------------

    /**
     * Object to represent a monetary amount incorporating both a currency
     * and a default rounding strategy.
     * 
     * @param amount The quantity of the monetary unit. Any fractional amounts
     * beyond the number of fractional digits of the currency will be lost
     * when the rounding mode is applied.
     * 
     * @param currency The 3-letter ISO currency code.  Examples:
     *     <ul>
     *     <li>CAD  Canadian Dollar</li>
     *     <li>JPY  Japanese Yen</li>
     *     <li>USD  US Dollar</li>
     *     </ul>
     *
     * @param roundingMode Defines how fractional units should be handled.
     */
    public Money(final BigDecimal amount, final Currency currency, final RoundingMode roundingMode) {
        
        if (null == amount) {
            throw new IllegalArgumentException("Money amount may not be null");
        }
        
        if (null == currency) {
            throw new IllegalArgumentException("Money currency may not be null");
        }

        if (null == roundingMode) {
            throw new IllegalArgumentException("Money roundingMode may not be null");
        }
        
        this.currency = currency;
        this.roundingMode = roundingMode;
        
        // Remove any excess decimal places in the amount according to roundingMode rules
        BigDecimal partiallyTruncatedAmount = amount.setScale(currency.getDefaultFractionDigits() * 2 + 1, RoundingMode.HALF_UP);
        this.amount = partiallyTruncatedAmount.setScale(currency.getDefaultFractionDigits(), roundingMode);
        
    }
    

    /**
     * Creates a Money object and applies the system default rounding mode.
     * 
     * @param amount See Money constructor
     * @param currency See Money constructor
     */
    public Money(final BigDecimal amount, final Currency currency) {
        this(amount, currency, APP_CONFIG.getDefaultRoundingMode());
    }

    
    /**
     * Creates a Money object and applies the system default rounding mode and currency
     * 
     * @param amount See Money constructor
     */
    public Money(final BigDecimal amount) {
        this(amount, APP_CONFIG.getDefaultCurrency(), APP_CONFIG.getDefaultRoundingMode());
    }


    // -------------- String Constructors ---------------------------

    public Money(final String amount, final Currency currency, final RoundingMode roundingMode) {
        this(new BigDecimal(amount), currency, roundingMode);
    }
    
    public Money(final String amount, final Currency currency) {
        this(new BigDecimal(amount), currency, APP_CONFIG.getDefaultRoundingMode());
    }
    
    public Money(final String amount) {
        this(new BigDecimal(amount), APP_CONFIG.getDefaultCurrency(), APP_CONFIG.getDefaultRoundingMode());
    }
    
    
    // -------------- Money Constructors ---------------------------

    public Money(final Money srcMoney) {
        this.amount = srcMoney.getAmount();
        this.currency = srcMoney.getCurrency();
        this.roundingMode = srcMoney.getRoundingMode();
    }




    /*-------------------------------
     * Accessors
     *-------------------------------
     */
    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }


    /*-------------------------------
     * Arithmetic
     *-------------------------------
     */

    /**
     * Arithmetic operations should only be performed on units of the same
     * currency.
     *
     * @param moneyToCheck The money object whose currency should be compared
     * to this object.
     * @return true if the amount to add is the same currency as the money
     * being added to.
     */
    public boolean isSameCurrency(final Money moneyToCheck) {
        return currency.equals(moneyToCheck.getCurrency());
    }

    
    private void assertSameCurrencyAs(final Money arg) {
        if (!isSameCurrency(arg)) {
            throw new CurrencyMismatchRuntimeException(currency, arg.getCurrency());
        }
    }

    /**
     * Add a monetary amount to a base amount. Only for monetary amounts with
     * the same currency. The new monetary amount is created with the same
     * rounding mode of the base amount, regardless of the rounding mode of the 
     * added amount.
     * 
     * @param moneyToAdd The money to add to the base amount
     * 
     * @return A new money object with the quantity equal to the sum of
     * the base and the additional amount and the rounding mode equal to
     * the rounding mode of the base amount.
     */
    public Money add(final Money moneyToAdd) {
        assertSameCurrencyAs(moneyToAdd);
        final BigDecimal sum = getAmount().add(moneyToAdd.getAmount());
        return new Money(sum, getCurrency(), getRoundingMode());
    }

    
    /**
     * Subtract a monetary amount from a base amount. Only for monetary amounts
     * with the same currency. The new monetary amount is created with the same
     * rounding mode of the base amount, regardless of the rounding mode of the 
     * added amount.
     * 
     * @param moneyToSubtract The money to subtract from the base amount
     * 
     * @return A new money object with the quantity equal to the difference
     * between the base amount and the supplied argument. The rounding mode
     * is equal to the rounding mode of the base amount.
     */
    public Money subtract(final Money moneyToSubtract) {
        assertSameCurrencyAs(moneyToSubtract);
        final BigDecimal result = getAmount().subtract(moneyToSubtract.getAmount());
        return new Money(result, getCurrency(), getRoundingMode());
    }

    
    /**
     * Create a new monetary object with where the amount is the product of
     * the base amount and the multiplicand. Currency and rounding mode of
     * the original amount carry through to the new amount.
     * 
     * @param multiplicand The amount to multiply the base amount by for forming
     * the new monetary object.
     * 
     * @return A monetary object with the same currency and rounding mode of the
     * original object, but an amount multiplied by the multiplicand.
     */
    public Money multiply(final double multiplicand) {
        final BigDecimal result = getAmount().multiply(BigDecimal.valueOf(multiplicand));
        return new Money(result, getCurrency(), getRoundingMode() );
    }


    /**
     * Rounded according to rounding rules of the Money object.
     * 
     * @param denominator The amount to divide by
     * @return The currency and rounding mode carry through.
     */
    public Money divide(final BigDecimal denominator) {

        BigDecimal truncatedResult = amount.divide(
                denominator,
                currency.getDefaultFractionDigits(),
                roundingMode);

        return new Money(truncatedResult, currency, roundingMode);

    }


    /**
     * A split is a partitioning of a Monetary amount into a specified set of
     * containers such that the sum of the amounts in all containers exactly
     * equals the original amount. The amounts distributed to each container
     * is done on a "fair" basis and determined by the DivideType used when
     * retrieving the Collection of partitions.
     * 
     * @param containers The number of partitions the monetary amount
     * should be split into.
     * 
     * @return A wrapper object which provides mechanisms for querying the
     * results of the split and for iterating through the split with a
     * selection of "DivideType"s.
     */
    public Split split(final int containers) {
        if (containers <= 0) {
            throw new ArithmeticException("Split only on positive integers");
        }
        return new Split(this, containers);
    }



    /*-------------------------------
     * Comparable Interface implementation
     *-------------------------------
     */

    @Override
    public int compareTo(final Money arg) {
        assertSameCurrencyAs(arg);
        return amount.compareTo(arg.getAmount());
    }

    
    /*-------------------------------
     * Comparison operators
     *-------------------------------
     */

    public boolean greaterThan(final Money arg) {
        return compareTo(arg) > 0;
    }

    public boolean lessThan(final Money arg) {
        return compareTo(arg) < 0;
    }



    /*-------------------------------
     * equals and hashCode
     *-------------------------------
     */

    @Override
    public int hashCode() {
        return amount.hashCode() + currency.hashCode();
    }

    /**
     * Note that rounding mode does not affect equality whereas currency
     * and magnitude do.
     * 
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Money other = (Money) obj;
        
        if (!amount.equals(other.amount)) {
            return false;
        }
        
        return currency.equals(other.currency);

    }


    /*-------------------------------
     * toString methods
     *-------------------------------
     */

    @Override
    public String toString() {
        return getAmount().toPlainString();
    }

    public String toStringWithCurrency() {
        return currency.toString() + " " + toString();
    }
    
    public String toDebugString() {
        return toStringWithCurrency() + "[" + roundingMode.toString() + "]";
    }
    
    
}