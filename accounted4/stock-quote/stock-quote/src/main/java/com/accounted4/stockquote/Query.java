package com.accounted4.stockquote;

import com.accounted4.stockquote.api.QuoteAttribute;
import com.accounted4.stockquote.api.QuoteService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.cli.*;


/**
 * Command Line Interface utility for interactive quick quotes.
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class Query {

    
    private static String VERSION = "0.1";
    
    
    /* ---------------------------
     * Interactive
     *     -showServices
     *     -showAtributes
     *     -service <serviceName> -symbols <symbol>[,<symbol>] -attributes <attr>[,<attr>]
     *     -help
     * ---------------------------
     */
    public static void main(String[] args) {
    
        // Define command line options
        
        Option help = new Option( "help", "print this message" );
        Option version = new Option( "version", "print the version information and exit" );
        Option showServices = new Option( "showServices", "list the stock quoting services configured for queries" );
        Option showAttributes = new Option( "showAttributes", "list attributes that may be retrieved" );        
        
        Option service = OptionBuilder.withArgName( "serviceName" )
                                .hasArg()
                                .withDescription(  "query the given service" )
                                .create( "service" );
        
        Option symbols = OptionBuilder.withArgName( "tickerSymbols" )
                                .hasArg()
                                .withDescription(  "comma separated list of ticker symbols to query" )
                                .create( "symbols" );
        
        Option attributes = OptionBuilder.withArgName( "queryAttributes" )
                                .hasArg()
                                .withDescription(  "comma separated list of attributs to query for, ex last" )
                                .create( "attributes" );
        
        OptionGroup optionGroup = new OptionGroup();
        optionGroup.addOption(help);
        optionGroup.addOption(version);
        optionGroup.addOption(showServices);
        optionGroup.addOption(showAttributes);
        
        
        Options options = new Options();
        options.addOptionGroup(optionGroup);

        options.addOption(service);
        options.addOption(symbols);
        options.addOption(attributes);
        
        HelpFormatter formatter = new HelpFormatter();
        
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = null;
        
        
        // Unrecognized command line options, print help and escape
        try {
            cmd = parser.parse(options, args);
        } catch(ParseException pe) {
            System.out.println("Malformed command");
            formatter.printHelp( "Query", options );
            return;
        }

        
        // No options or asking for help, print help and escape
        if (cmd.getOptions().length <= 0 || cmd.hasOption(help.getOpt())) {
            formatter.printHelp( "Query", options );
            return;
        }
        
        
        if (cmd.hasOption(version.getOpt())) {
            System.out.println("version: " + VERSION);
            return;
        }

        
        
        QueryBuilder query = new QueryBuilder();
        

        // Show Services option
        
        if (cmd.hasOption(showServices.getOpt())) {
            List<QuoteService> supportedQuoteServices = query.getSupportedQuoteServices();
            System.out.println("Discovered services: ");
            for (QuoteService quoteService : supportedQuoteServices) {
                System.out.println("  " + quoteService.getServiceName());
            }
            return;
        }
        
        
        // Supported query attributes
        
        if (cmd.hasOption(showAttributes.getOpt())) {
            System.out.println("Supported query attributes: ");
            for (QuoteAttribute attribute : QuoteAttribute.values()) {
                System.out.println("  " + attribute);
            }
            return;
        }
        

        String selectedService = cmd.getOptionValue(service.getOpt());
        String selectedSymbols = cmd.getOptionValue(symbols.getOpt());
        
        // If no query attributes were specified, default to LAST_TRADE_PRICE
        String selectedAttributes = cmd.getOptionValue(attributes.getOpt(), QuoteAttribute.LAST_TRADE_PRICE.toString());

        // Find the named quote service
        List<QuoteService> supportedQuoteServices = query.getSupportedQuoteServices();
        for (QuoteService quoteService : supportedQuoteServices) {
            
            if (!quoteService.getServiceName().equalsIgnoreCase(selectedService)) {
                continue;
            }
            
            // Build the list of symbols based on entered csv
            List<String> enteredSymbols = Arrays.asList(selectedSymbols.split(","));
            
            // Build the list of attributes based on entered csv
            ArrayList<QuoteAttribute> attrList = new ArrayList<>();
            for (String s : selectedAttributes.split(",")) {
                try {
                    attrList.add(QuoteAttribute.valueOf(s));
                } catch (IllegalArgumentException iae) {
                    System.out.println("Ignoring unrecognized attribute: " + s);
                }
            }
            
            // Query the service
            List<HashMap<QuoteAttribute, String>> result = quoteService.executeQuery(enteredSymbols, attrList);

            // Dump the result
            for (HashMap<QuoteAttribute, String> line : result) {
                System.out.println();
                for ( Entry<QuoteAttribute, String> entry : line.entrySet() ) {
                    System.out.println("  " + entry.getKey().toString() + " = " + entry.getValue());
                }
            }
            
            return;
            
        }        

        
    }
    
    
}
