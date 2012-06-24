package com.accounted4.stockquote.tmx;


import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.TreeSet;



/**
 * A collection of the available options (CALL and PUT) available for
 * a given company.
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class OptionChain {

    private static final NumberFormat numberFormat = NumberFormat.getInstance();
    static { 
        numberFormat.setMinimumFractionDigits(2); 
        numberFormat.setMaximumFractionDigits(2); 
    } 
    
    
    private String symbol;
    private BigDecimal lastPrice;
    private String queryTime;
    private final TreeSet<Option> calls = new TreeSet<>();
    private final TreeSet<Option> puts = new TreeSet<>();
    
    
    public OptionChain(String symbol, BigDecimal lastPrice, String queryTime) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
        this.queryTime = queryTime;
    }
    
    
    public void addOption(Option option) {
        
        if (!option.getSymbol().equals(symbol)) {
            throw new IllegalArgumentException("Attempt to add option with symbol: " + option.getSymbol() +
                    " to an option chain for: " + getSymbol() );
        }
        
        if (option.getOptionType().equals(OptionType.CALL)) {
            calls.add(option);
        } else {
            puts.add(option);
        }
        
    }

    
    @Override
    public String toString() {
        
        StringBuilder result = new StringBuilder();
        result.append("Symbol: ").append(getSymbol()).append("\n");
        result.append("Last Price: ").append(getDisplayLastPrice()).append("\n");
        result.append("Query Time: ").append(getQueryTime()).append("\n");

        result.append("\n").append(OptionType.CALL).append("\n");
        for (Option option : calls) {
            result.append("  ").append(option.toString()).append("\n");
        }
        
        result.append("\n").append(OptionType.PUT).append("\n");
        for (Option option : puts) {
            result.append("  ").append(option.toString()).append("\n");
        }
        
        return result.toString();
    }
    

    public String getDisplayLastPrice() {
        return numberFormat.format(lastPrice);
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
     * @return the queryTime
     */
    public String getQueryTime() {
        return queryTime;
    }


    /**
     * @param queryTime the queryTime to set
     */
    public void setQueryTime(String queryTime) {
        this.queryTime = queryTime;
    }

    // TODO: return copy. or not.
    public TreeSet<Option> getCalls() {
        return calls;
    }

    // TODO: return copy
    public TreeSet<Option> getPuts() {
        return puts;
    }

}
