package com.accounted4.stockquote.api;


/**
 * Any error accessing the quote service can be wrapped in a QuoteServiceException.
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class QuoteServiceException extends Exception {

    public QuoteServiceException(String msg) {
        super(msg);
    }
    
}
