package com.accounted4.stockquote;

import com.accounted4.stockquote.api.QuoteAttribute;
import com.accounted4.stockquote.api.QuoteService;
import com.accounted4.stockquote.api.QuoteServiceException;
import java.util.*;


/**
 * Prepare inputs and invoke an appropriate Stock Quoting Service.
 * Query criteria include:
 * <ul>
 *   <li>serviceName</li>
 *   <li>list of securities</li>
 *   <li>list of attributes to query for each security</li>
 * </ul>
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class QueryBuilder {

    
    // Quote provider to query for information
    private QuoteService selectedQuoteService;
    
    // Type of information to query for (last price, company name, etc)
    private List<QuoteAttribute> quoteAttributes;
    
    // Securities for which to query for information. Note that the security name is dependent
    // on the service and the exchange. For example, quoting Yahoo for BMO would query the
    // NYSE by default. One would need to use BMO.TO for the Yahoo service to check at the TSX
    private List<String> securityList;
    
    // Available quote services are discovered on the classpath via meta-inf/services
    private static ServiceLoader<QuoteService> quoteServiceLoader;
    
    

    /**
     * Initialize a new Query builder with a default query service.
     */
    public QueryBuilder() {
        
        securityList = new ArrayList<>();
        quoteAttributes = new ArrayList<>();
        
        quoteServiceLoader = ServiceLoader.load(QuoteService.class);
        
        // Select the first service we can find as a default service, if it exists
        Iterator<QuoteService> serviceIterator = quoteServiceLoader.iterator();
        if (serviceIterator.hasNext()) {
            selectedQuoteService = serviceIterator.next();
        }
        
    }
    
    
    /* ---------------------------
     * Quote Service API
     * ---------------------------
     */
    

    /**
     * Discover Quote services on the classpath.
     * 
     * @return A list of available services
     */
    public List<QuoteService> getSupportedQuoteServices() {
    
        ArrayList<QuoteService> result = new ArrayList<>();
        
        Iterator<QuoteService> serviceIterator = quoteServiceLoader.iterator();
        while (serviceIterator.hasNext()) {
            result.add(serviceIterator.next());
        }

        return result;
        
    }

    
    /**
     * The quote service which will be used if the query were to be 
     * executed at this stage.
     * 
     * @return The quote service to be used for requesting quotes. It is possible
     * for the result to be null;
     */
    public QuoteService getSelectedQuoteService() {
        return selectedQuoteService;
    }

    
    /**
     * Specify the quoting service to use for queries.
     * 
     * @param service The quoting service to use
     */
    public void setSelectedQuoteService(QuoteService service) {
        selectedQuoteService = service;
    }
    

    /**
     * Specify the name of the quoting service to use for queries.
     * 
     * @param serviceName The name of the quoting service
     * 
     * @throws QuoteServiceException If no quoting service matching the given
     * name could be found among the services on the classpath.
     */
    public void setSelectedQuoteService(String serviceName) throws QuoteServiceException {
        
        List<QuoteService> supportedQuoteServices = getSupportedQuoteServices();
        for (QuoteService service : supportedQuoteServices) {
            if (service.getServiceName().equalsIgnoreCase(serviceName)) {
                setSelectedQuoteService(service);
                return;
            }
        }

        throw new QuoteServiceException("Unknown service name: " + serviceName + "\n" +
                "Known services: " + supportedQuoteServices);
        
    }
    
        
    
    /* ---------------------------
     * Attribute API
     * ---------------------------
     */
    
    /**
     * The list of attributes for which the quote service will be queried. The list is ordered
     * and could contain the same attribute multiple times if the list was build in that manner.
     * 
     * @return The list of query attributes (ex last price, company name, ...).
     */
    public List<QuoteAttribute> getQuoteAttributes() {
        return new ArrayList<>(quoteAttributes);
    }
    
    
    /**
     * Adds an attribute to the list of quote criteria to query for..
     * 
     * @param attribute The attribute specifying the information to retrieve from the quoting
     * service when the query is executed.
     */
    public void addQuoteAttribute(QuoteAttribute attribute) {
        quoteAttributes.add(attribute);
    }
    
    
    /**
     * Remove all attributes from the query builder. Perhaps useful when re-using the
     * QueryBuilder for multiple queries with different attribute lists: clear the list
     * between queries.
     */
    public void clearQuoteAttributes() {
        quoteAttributes.clear();
    }
    
    
    /* ---------------------------
     * Security API
     * ---------------------------
     */

    /**
     * The list of ticker symbols to use when querying the quoting service.
     * 
     * @return  A list of strings representing ticker symbols in a format that is
     * should be structured for the underlying query service.
     */
    public List<String> getSecurityList() {
        return new ArrayList<>(securityList);
    }
    
    
    /**
     * The ticker symbol to use when querying the service.
     * 
     * @param security A ticker symbol to add to the list of securities to query.
     */
    public void addSecurity(String security) {
        securityList.add(security);
    }

    
    /**
     * Remove all ticker symbols from the query builder. Perhaps useful when re-using the
     * QueryBuilder for multiple queries with different security lists: clear the list
     * between queries.
     */
    public void clearSecurityList() {
        securityList.clear();
    }
    
    
    /* ---------------------------
     * Execute
     * ---------------------------
     */
    
    /**
     * After building the query by choosing a service, adding securities, and specifying the
     * attributes to query for, the service can be queried for the information.
     * 
     * @return a list of results for each ticker symbol. Each list item is itself a
     * list of the resultant values for the queried attributes. Example:
     * 
     *   [ [SYMBOL, BMO.TO], [NAME, BANK OF MONTREAL], [LAST_PRICE, 58.50] ]
     *  ,[ [SYMBOL, MSFT], [NAME, Microsoft], [LAST_PRICE, 30.25] ]
     * 
     * @throws QuoteServiceException 
     */
    public List<HashMap<QuoteAttribute, String>> executeQuery() throws QuoteServiceException {
        
        if (null == selectedQuoteService) {
            throw new QuoteServiceException("No quote service selected");
        }
        
        if (quoteAttributes.isEmpty()) {
            throw new QuoteServiceException("No query attributes selected");
        }
        
        if (securityList.isEmpty()) {
            throw new QuoteServiceException("No securities selected");
        }
        
        return selectedQuoteService.executeQuery(securityList, quoteAttributes);
        
    }

    
}
