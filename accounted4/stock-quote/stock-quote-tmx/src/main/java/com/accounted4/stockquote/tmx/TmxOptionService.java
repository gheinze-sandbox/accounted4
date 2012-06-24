package com.accounted4.stockquote.tmx;


import java.io.IOException;
import org.xml.sax.SAXException;

/**
 * Query the Montreal Stock Exchange for Option information.
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class TmxOptionService {
    
    private static String SERVICE_NAME = "TMX Option Quotes";
    
    //TODO: get url from a properties file: url info has changed in the past, shouldn't require a recompile
    private static final String BASE_URL = "http://www.m-x.ca/nego_cotes_en.php?pageTopPrint=yes&symbol=${symbol}";


    public String getServiceName() {
        return SERVICE_NAME;
    }
   
    
    /**
     * Query the Montreal exchange for the option chain of a given symbol. Parse out the
     * html response and return a chain object.
     * 
     * @param symbol
     * @return
     * @throws SAXException
     * @throws IOException 
     */
    public OptionChain getOptionChain(String symbol) throws SAXException, IOException {
        
        String urlString = BASE_URL.replaceAll("\\$\\{symbol\\}", symbol);
        System.out.println("Query url: " + urlString);
        
        //urlString = "file:///C:/temp/tmxOptionResult.html";
        
        HtmlSaxParser saxParser = new HtmlSaxParser();
        TmxOptionPageHandler tmxOptionPageHandler = new TmxOptionPageHandler(symbol);
        saxParser.setContentHandler(tmxOptionPageHandler);
        saxParser.parse(urlString);
        
        return tmxOptionPageHandler.getOptionChain();
    }
 
    
    /**
     * Give it a test run.
     * 
     * @param args
     * @throws SAXException
     * @throws IOException 
     */
    public static void main(String[] args) throws SAXException, IOException {
        
        TmxOptionService service = new TmxOptionService();
        OptionChain optionChain = service.getOptionChain(args[0]);
        System.out.println("\n\n\n");
        System.out.println(optionChain);
    }
    
}
