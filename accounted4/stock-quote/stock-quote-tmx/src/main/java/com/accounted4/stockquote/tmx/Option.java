package com.accounted4.stockquote.tmx;


import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Objects;



/**
 * POJO representing a stock option (CALL or PUT)
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class Option implements Comparable {

    private static final NumberFormat numberFormat = NumberFormat.getInstance();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd");
    
    static { 
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
    }
    
    private OptionType optionType;
    private String symbol;
    private GregorianCalendar expiryDate;
    private BigDecimal strikePrice;
    
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal lastPrice;

    /**
     * Assume ZERO prices as indicators of unknown values
     * 
     */
    public Option(
             OptionType optionType
            ,String symbol
            ,GregorianCalendar expiryDate
            ,BigDecimal strikePrice
            ,BigDecimal bidPrice
            ,BigDecimal askPrice
            ,BigDecimal lastPrice
            ) {
        
        this.optionType = optionType;
        this.symbol = symbol;
        this.expiryDate = expiryDate;
        this.strikePrice = strikePrice;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.lastPrice = lastPrice;
        
    }

    
    @Override
    public String toString() {
        return getOptionType() + " " + getSymbol() + " " + getDisplayExpiryDate() + " " + getDisplayStrikePrice() +
                " bid: " + getDisplayBidPrice() + " ask: " + getDisplayAskPrice() + " last: " + getDisplayLastPrice();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Option other = (Option) obj;
        if (this.optionType != other.optionType) {
            return false;
        }
        if (!Objects.equals(this.symbol, other.symbol)) {
            return false;
        }
        if (!Objects.equals(this.expiryDate, other.expiryDate)) {
            return false;
        }
        if (!Objects.equals(this.strikePrice, other.strikePrice)) {
            return false;
        }
        return true;
    }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.optionType != null ? this.optionType.hashCode() : 0);
        hash = 83 * hash + Objects.hashCode(this.symbol);
        hash = 83 * hash + Objects.hashCode(this.expiryDate);
        hash = 83 * hash + Objects.hashCode(this.strikePrice);
        return hash;
    }
    


    /** 
     * Order by:  SYMBOL, OPTION_TYPE, EXPIRY_DATE, STRIKE_PRICE
     * 
     * @param o
     * @return 
     */
    @Override
    public int compareTo(Object o) {

        if (null == o) {
            return 1;
        }
        
        Option other = (Option)o;
        
        int result = getSymbol().compareTo(other.getSymbol());
        if (0 != result) { return result; }

        result = getOptionType().compareTo(other.getOptionType());
        if (0 != result) { return result; }

        result = getExpiryDate().compareTo(other.getExpiryDate());
        if (0 != result) { return result; }

        return getStrikePrice().compareTo(other.getStrikePrice());

    }
    
    
    
    
    public String getDisplayExpiryDate() {
        return dateFormat.format(expiryDate.getTime());
    }

    public String getDisplayStrikePrice() {
        return numberFormat.format(getStrikePrice());
    }

    public String getDisplayBidPrice() {
        return numberFormat.format(getBidPrice());
    }

    public String getDisplayAskPrice() {
        return numberFormat.format(getAskPrice());
    }

    public String getDisplayLastPrice() {
        return numberFormat.format(getLastPrice());
    }


    /**
     * @return the optionType
     */
    public OptionType getOptionType() {
        return optionType;
    }


    /**
     * @param optionType the optionType to set
     */
    public void setOptionType(OptionType optionType) {
        this.optionType = optionType;
    }


    /**
     * @return the expiryDate
     */
    public GregorianCalendar getExpiryDate() {
        return expiryDate;
    }


    /**
     * @param expiryDate the expiryDate to set
     */
    public void setExpiryDate(GregorianCalendar expiryDate) {
        this.expiryDate = expiryDate;
    }


    /**
     * @return the strikePrice
     */
    public BigDecimal getStrikePrice() {
        return strikePrice;
    }


    /**
     * @param strikePrice the strikePrice to set
     */
    public void setStrikePrice(BigDecimal strikePrice) {
        this.strikePrice = strikePrice;
    }


    /**
     * @return the bidPrice
     */
    public BigDecimal getBidPrice() {
        return bidPrice;
    }


    /**
     * @param bidPrice the bidPrice to set
     */
    public void setBidPrice(BigDecimal bidPrice) {
        this.bidPrice = bidPrice;
    }


    /**
     * @return the askPrice
     */
    public BigDecimal getAskPrice() {
        return askPrice;
    }


    /**
     * @param askPrice the askPrice to set
     */
    public void setAskPrice(BigDecimal askPrice) {
        this.askPrice = askPrice;
    }


    /**
     * @return the lastPrice
     */
    public BigDecimal getLastPrice() {
        return lastPrice;
    }


    /**
     * @param lastPrice the lastPrice to set
     */
    public void setLastPrice(BigDecimal lastPrice) {
        this.lastPrice = lastPrice;
    }


    /**
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }


    /**
     * @param symbol the symbol to set
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

}
