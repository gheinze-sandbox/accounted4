package com.accounted4.stockquote.tmx;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * SAX parsing:
 * 
 * Parse out the response page received from an option query to the TMX:
 * ex query:  http://www.m-x.ca/nego_cotes_en.php?pageTopPrint=yes&symbol=cm
 * 
 * 1. Scan until the text: "Last update:" is encountered, and grab the string, this gives quote time
 * 
 * 2. Keep scanning until the text for one of the "price" fields is found. The text will set the type
 *    of the price field (last, bid, ask, ...) for the underlying commodity. This will be picked up
 *    via the sub-element <STRONG>. Start collection with this tag, end collection at end of tag.
 *    If the "30-Day Historical Volatility:" has been collected, then collection of the underlying
 *    commodity information is considered complete and "priceState" is cleared.
 * 
 * 3. Keep scanning until the text "Calls" is encountered. This sets the optionType state. Now, when
 *    <TR> elements are encountered, they are considered rows for CALL options.
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class TmxOptionPageHandler extends DefaultHandler {

    private boolean collectUnderlyingCommodityPrice = false;
    private String underlyingCommodoityPriceFieldToCollect = "";
    
    private OptionType optionType = null;
    private boolean optionRowProcessing = false;
    private boolean optionColumnProcessing = false;
    private int optionColumnIndex = -1;
    
    private String[] optionBuilder = new String[6];
    private HashMap<String, String> underlyingCommodityPriceInfo = new HashMap<>();
    
    private String symbol;
    private ArrayList<Option> optionList = new ArrayList<>();
    
    
    public TmxOptionPageHandler(String symbol) {
        this.symbol = symbol;
    }
    
    
    
    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {

        if ( !underlyingCommodoityPriceFieldToCollect.isEmpty() && qName.equalsIgnoreCase("STRONG")) {
            collectUnderlyingCommodityPrice = true;
            return;
        }
        
        // If we haven't hit the option data, then the rest of the checks are moot.
        if (null == optionType) {
            return;
        }
        
        // Entering a new row of option data, initialize all option variables
        if ( qName.equalsIgnoreCase("TR") && null != attributes.getValue("title") ) {
            String title = attributes.getValue("title");
            optionBuilder = new String[6];
            optionBuilder[0] = title;
            optionRowProcessing = true;
            optionColumnIndex = -1;
            return;
        }

        // Moving on to a new column for the option data, increment the column
        if (optionRowProcessing && qName.equalsIgnoreCase("TD")) {
            optionColumnIndex++;
            optionColumnProcessing = true;
            return;
        }

    }


    @Override
    public void endElement(String uri, String localName,
                           String qName) throws SAXException {

        // Completed gathering all the info for an option, wrap it up and exit processing mode
        if (optionRowProcessing && qName.equalsIgnoreCase("TR")) {
            optionRowProcessing = false;
            addOption(optionBuilder);
            //System.out.println(optionBuilder[0] + " | " + optionBuilder[1] + " | " + optionBuilder[2] + " | " + optionBuilder[3] + " | " + optionBuilder[4] + " | " + optionBuilder[5]);
            return;
        }

        // Completed gathering option data for a column, exit column mode processing
        if (optionColumnProcessing && qName.equalsIgnoreCase("TD")) {
            optionColumnProcessing = false;
            return;
        }

        // Completed gathering some info for the underlying commodity
        if (collectUnderlyingCommodityPrice && qName.equalsIgnoreCase("STRONG")) {
            collectUnderlyingCommodityPrice = false;
            if (ULC_VOLATILITY_PREFIX.equals(underlyingCommodoityPriceFieldToCollect)) {
                underlyingCommodoityPriceFieldToCollect = "";
                //System.out.println(underlyingCommodityPriceInfo);
            }
            return;
        }

    }


    // ULC: Underlying Commodity. Not the option itself.
    
    private static final String ULC_LAST_UPDATE_PREFIX = "Last update:";
    
    private static final String ULC_LAST_PRICE_PREFIX = "Last Price:";
    private static final String ULC_NET_CHANGE_PREFIX = "Net Change:";
    private static final String ULC_BID_PRICE_PREFIX = " Bid Price:";
    private static final String ULC_ASK_PRICE_PREFIX = "Ask Price:";
    private static final String ULC_VOLATILITY_PREFIX = "30-Day Historical Volatility:";
    private static final String ULC_QUERY_TIME = "Query Time";
    
    private static final String CALLS = "Calls";
    private static final String PUTS = "Puts";
    

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {

        
        String text = new String(ch, start, length).trim();

        if (text.equals(CALLS)) {
            optionType = OptionType.CALL;
            // System.out.println("Entering CALL section");
            return;
            
        } else if (text.equals(PUTS)) {
            optionType = OptionType.PUT;
            // System.out.println("Entering PUT section");
            return;
            
        } else if (text.startsWith(ULC_LAST_UPDATE_PREFIX)) {
            // The rest of the text has info about the query time
            String updateString = text.substring(ULC_LAST_UPDATE_PREFIX.length()).trim();
            underlyingCommodityPriceInfo.put(ULC_QUERY_TIME, updateString);
            // System.out.println(updateString);
            return;
            
        // Change state of what to collect in the underlying commodity: the data is under the sub-element <STRONG>, not here
        } else if (text.startsWith(ULC_LAST_PRICE_PREFIX)) {
            underlyingCommodoityPriceFieldToCollect = ULC_LAST_PRICE_PREFIX;
        } else if (text.startsWith(ULC_NET_CHANGE_PREFIX)) {
            underlyingCommodoityPriceFieldToCollect = ULC_NET_CHANGE_PREFIX;
        } else if (text.startsWith(ULC_BID_PRICE_PREFIX)) {
            underlyingCommodoityPriceFieldToCollect = ULC_BID_PRICE_PREFIX;
        } else if (text.startsWith(ULC_ASK_PRICE_PREFIX)) {
            underlyingCommodoityPriceFieldToCollect = ULC_ASK_PRICE_PREFIX;
        } else if (text.startsWith(ULC_VOLATILITY_PREFIX)) {
            underlyingCommodoityPriceFieldToCollect = ULC_VOLATILITY_PREFIX;
            
        } else if (collectUnderlyingCommodityPrice) {
            // In a state for collecting a field of info for an underlying commodity. Could be split over
            // lines. Grab it and through it in a hash.
            String value = underlyingCommodityPriceInfo.get(underlyingCommodoityPriceFieldToCollect);
            underlyingCommodityPriceInfo.put(underlyingCommodoityPriceFieldToCollect, (null == value) ? text : value + text);
        }

        if (!optionRowProcessing || !optionColumnProcessing) {
            return;
        }
        
        if ( optionColumnIndex > 0 && optionColumnIndex < 6 ) {
            optionBuilder[optionColumnIndex] = text;
        }
        
    }

    /*
     * The raw data for an option row has been parsed out and stored as a set of strings.
     * Attempt to convert the strings into appropriate date and numeric formats to creae
     * an "Option" object. Store the created object into a collection.
     */
    private void addOption(String[] optionBuilder) {
        
        // Field format example:
        // Field 0 (strike): header=[ BMO   120616C44.00] body=[Open Interest: 0] delay=[5] fade=[off] 
        // Field 1 (bid)   : 10.000
        // Field 2 (ask)   : 10.150
        // Field 3 (last)  : 11.150
        
        // Chop off all the stuff upto the symbol
        String work = optionBuilder[0].replaceAll("^header=" + Pattern.quote("[") + "\\s", "");
        
        // Split the symbol from the rest of the string. Note the "spaces" in the returned document
        // are actually "&nbsp;" symbols (ie \u00A0) which is not picked up by "\\s" regex
        String symbolString = work.split("(\\p{Z})+")[0];
        work = work.substring(symbolString.length()).replaceAll("(\\p{Z})*", "");
        
        String year = "20" + work.substring(0, 2);
        String month = work.substring(2, 4);
        String day = work.substring(4, 6);
        String strikePriceString = work.substring(7, work.indexOf("]"));

        GregorianCalendar expiryDate;
        try {
            expiryDate = new GregorianCalendar( Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day) );
        } catch(NumberFormatException nfe) {
            expiryDate = new GregorianCalendar(1900, 0, 1);
        }
        
        BigDecimal strikePrice;
        try {
            strikePrice = new BigDecimal(strikePriceString);
        } catch(NumberFormatException nfe) {
            strikePrice = BigDecimal.ZERO;
        }
        
        BigDecimal bid;
        try {
            bid = new BigDecimal(optionBuilder[1]);
        } catch(NumberFormatException nfe) {
            bid = BigDecimal.ZERO;
        }
        
        BigDecimal ask;
        try {
            ask = new BigDecimal(optionBuilder[2]);
        } catch(NumberFormatException nfe) {
            ask = BigDecimal.ZERO;
        }
        
        BigDecimal last;
        try {
            last = new BigDecimal(optionBuilder[3]);
        } catch(NumberFormatException nfe) {
            last = BigDecimal.ZERO;
        }
        
        Option option = new Option(
             optionType
            ,symbol
            ,expiryDate
            ,strikePrice
            ,bid
            ,ask
            ,last
                );
        
        optionList.add(option);
        
    }
    
    
    /**
     * Don't call until after the parse has been done...
     * 
     * @return 
     */
    public OptionChain getOptionChain() {
        
        BigDecimal lastPrice = BigDecimal.ZERO;
        try {
            lastPrice = new BigDecimal( underlyingCommodityPriceInfo.get(ULC_LAST_PRICE_PREFIX) );
        } catch(NumberFormatException nfe) {
        }
        
        OptionChain optionChain = new OptionChain(symbol, lastPrice, underlyingCommodityPriceInfo.get(ULC_QUERY_TIME));
        for (Option option : optionList) {
            optionChain.addOption(option);
        }
        
        return optionChain;
        
    }

}
