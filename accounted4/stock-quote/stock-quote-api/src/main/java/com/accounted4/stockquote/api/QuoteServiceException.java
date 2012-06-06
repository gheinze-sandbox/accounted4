package com.accounted4.stockquote.api;


/**
 * Thrown by the quote service.
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class QuoteServiceException extends Exception {

    public QuoteServiceException(String msg) {
        super(msg);
    }
    
}
