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
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Data structure to represent the division of a monetary amount into a
 * collection of parts
 *
 * Example, $50.00/6 = $8.333...
 * 
 *   floor     = 8.33
 *   ceiling   = 8.34
 *   remainder = amount - (floor*denominator)
 *             = 50.00 - (8.33*6)
 *             = 50.00 - 49.98
 *             = 0.02
 *   smallestUnitSize = 0.01
 *   remainderUnitCount = 2
 * 
 *   partitions (equalized):        [8.34, 8.34, 8.33, 8.33, 8.33, 8.33]
 *   partitions (final adjustment): [8.34, 8.34, 8.34, 8.34, 8.34, 8.30]
 * 
 * @author Glenn Heinze 
 */
public final class Split {
    
    private final Money inputMoney;
    private final int bucketCount;

    private final BigDecimal floor;
    private final BigDecimal ceiling;
    private final BigDecimal remainder;
    private final BigDecimal smallestUnitSize;
    private final int remainderUnitCount;


    /**
     * Construct and pre-compute attributes of the Split.
     * 
     * @param inputMoney The Money from which the split is to be based
     * 
     * @param bucketCount The number of partitions this amount should
     * be divided into.
     */
    public Split(final Money inputMoney, final int bucketCount) {

        if (null == inputMoney) {
            throw new IllegalArgumentException("Money amount may not be null");
        }
        
        if (bucketCount <= 0) {
            throw new IllegalArgumentException("Number of buckets for the split must be > 0");
        }
        
        this.inputMoney = inputMoney;
        this.bucketCount = bucketCount;

        int fractionDigits = inputMoney.getCurrency().getDefaultFractionDigits();

        // The amount representing 1 unit of the fractional portion of the currency
        // ie for dollars: 0.01 (one cent), for yen: 1 (1 yen)
        smallestUnitSize = new BigDecimal(
                Double.toString(
                Math.pow(10, fractionDigits * -1)
                ));

        final BigDecimal bigDecimalBucketCount = new BigDecimal(bucketCount);

        // Do a straight forward divide, throw away the fractional units
        // This is the base amount for each container
        floor = inputMoney.getAmount().divide(
                bigDecimalBucketCount,
                fractionDigits,
                RoundingMode.DOWN );

        // Since we threw away fractional units, the amount in our return set may
        // be less then the actual amount.  Find out the difference.
        final BigDecimal recalculatedAmount =
                floor
                .multiply(bigDecimalBucketCount)
                .setScale(fractionDigits);
        remainder = inputMoney.getAmount().subtract(recalculatedAmount);

        // The ceiling will always be one higher than the floor, unless
        // we had an even divide in which case it is the same as the floor
        ceiling = BigDecimal.ZERO.equals(remainder) ?
                floor.add(BigDecimal.ZERO) :
                floor.add(smallestUnitSize);

        BigDecimal[] quotientRemainderPair = remainder.divideAndRemainder(smallestUnitSize);
        assert 0 == BigDecimal.ZERO.compareTo(quotientRemainderPair[1]) :
                "Remainder should be multiple of currency fractional units";

        remainderUnitCount = quotientRemainderPair[0].intValue();

    }


    /*-------------------------------
     * API
     *-------------------------------
     */
    
    /**
     * The least Monetary amount that can be found in any partition.
     * The difference between the floor and the ceiling can be at most
     * one fractional unit of the Money's currency. So for Canadian
     * dollar amounts, with two decimal places, the difference between
     * the floor and the ceiling can be at most $0.01
     * 
     * @return The least amount found in any partition of the split. Currency
     * and rounding mode match that of original amount.
     */
    public Money getFloor() {
        return new Money(floor, inputMoney.getCurrency(), inputMoney.getRoundingMode());
    }

    
    /**
     * The greatest Monetary amount that can be found in any partition.
     * The difference between the floor and the ceiling can be at most
     * one fractional unit of the Money's currency. So for Canadian
     * dollar amounts, with two decimal places, the difference between
     * the floor and the ceiling can be at most $0.01
     * 
     * @return The greatest amount found in any partition of the split. Currency
     * and rounding mode match that of original amount.
     */
    public Money getCeiling() {
        return new Money(ceiling, inputMoney.getCurrency(), inputMoney.getRoundingMode());
    }

    
    /**
     * A Monetary amount representing the remainder of the divide which will
     * always be less than the number of partitions and which will be distributed
     * in a "fair" manner over the partitions according to the DivideType.
     * 
     * @return The remainder amount with currency and rounding mode matching
     * that of original amount.
     */
    public Money getRemainder() {
        return new Money(remainder, inputMoney.getCurrency(), inputMoney.getRoundingMode());
    }

    
    /**
     * The number of partitions the remainder amount will be distributed
     * over.
     * 
     * @return remainder divided by the currency fractional unit
     */
    public int getRemainderUnitCount() {
        return remainderUnitCount;
    }
        
    
    /**
     * The partitions forming the split.
     * 
     * @param divideType Determinant of remainder distribution among partitions
     * 
     * @return The partitions with the original money "fairly" distributed.
     */
    public Collection<Money> getPartitions(final DivideType divideType) {
        
        return new AbstractCollection<Money>() {
            
            @Override
            public Iterator<Money> iterator() {
                return new SplitIterator(divideType);
            }
            
            @Override
            public int size() {
                return bucketCount;
            }
            
        };
                
    }
        
    
    /* ------------------------------------------------------- */
    
    
    private final class SplitIterator implements Iterator<Money> {

        private int currentIndex = 0;
        private SplitAmountCalculator splitAmountCalculator;

        private SplitIterator(final DivideType divideType) {
            switch (divideType) {
                case Equalized:
                    splitAmountCalculator = new EqualizedSplitAmount();
                    break;
                case Final_Adjustment:
                    splitAmountCalculator = new FinalAdjustmentSplitAmount();
                    break;
                default:
                    assert false : "Unknown Money divide type";
            }
        }
        
        @Override
        public boolean hasNext() {
            return currentIndex < bucketCount;
        }
        
        @Override
        public Money next() {
            if (!hasNext()) {
                throw new NoSuchElementException("getEqualizedIterator");
            }
            return new Money(
                    splitAmountCalculator.nextAmount(currentIndex++),
                    inputMoney.getCurrency(),
                    inputMoney.getRoundingMode());
        }

        // Not supported
        @Override
        public void remove() {
        }
    }
        
        
    
    /*
     * The amount to return in the iterator is dependent on how the remainder
     * of the divides is to be distributed.
     */
    private interface SplitAmountCalculator {
        BigDecimal nextAmount(final int index);
    }

    
    /*
     * Ceiling amounts will be returned in the first elements of the iterator
     * until all the "remainder" units are used up, at which point floor
     * amounts will be returned until all partitions have been created.
     */
    private class EqualizedSplitAmount implements SplitAmountCalculator {
        @Override
        public BigDecimal nextAmount(final int index) {
            return index < remainderUnitCount ? ceiling : floor;
        }
    }

    
    /*
     * Ceiling amounts will be returned for all (n - 1) partitions. The
     * final partition could be smaller: it is is the remainder of the 
     * original amount - all partitions already allocated.
     */
    private class FinalAdjustmentSplitAmount implements SplitAmountCalculator {
        @Override
        public BigDecimal nextAmount(final int index) {
            if (index == bucketCount - 1) {
                BigDecimal multiplicand = new BigDecimal(bucketCount - 1);
                return inputMoney.getAmount().subtract(ceiling.multiply(multiplicand));
            }
            return ceiling;
        }
    }


}
