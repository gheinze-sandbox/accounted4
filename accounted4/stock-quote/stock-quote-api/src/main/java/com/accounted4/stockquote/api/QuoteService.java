package com.accounted4.stockquote.api;

import java.util.HashMap;
import java.util.List;


/**
 * Implementors of a a stock quote service should:
 *   o implement the QuoteService interface
 *   o add the fully qualified name of the implementing class into the file:
 *       META-INF.services.com.accounted4.stockquote.api.QuoteService
 *   o add the jar to the classpath of the stock-quote executable
 * See stock-quote-yahoo for an example implementation
 * 
 * @author gheinze
 */
public interface QuoteService {


    /**
     * Provide a name for the service that provides stock quotes.
     * The name is used to distinguish it from other possible implementations.
     * 
     * @return Name of the service.
     */
    String getServiceName();

    /**
     * Query this service which provides stock quotes. Query for each security supplied in the parameter list,
     * and for each one of the securities, retrieve information for each attributed requested in the quoteAttributes
     * list.
     * 
     * @param securityList List of securities to query
     * @param quoteAttributes A list of attribute to look up for each security
     * 
     * @return A list of key-value pairs (query attribute -> resultant value) for each security in the security list.
     * The result list may be smaller than the supplied list if the security was not found at the quote service.
     */
    List<HashMap<QuoteAttribute, String>> executeQuery(List<String> securityList, List<QuoteAttribute> quoteAttributes);
    
}
