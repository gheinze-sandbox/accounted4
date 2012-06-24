package com.accounted4.stockquote.tmx;


import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;


/**
 * Use Nekohtml as the sax parser in order to transform the html
 * page into a xml format parsable by xerces.
 * 
 * @author Glenn Heinze <glenn@gheinze.com>
 */
public class HtmlSaxParser extends AbstractSAXParser {

    public HtmlSaxParser() {
        super(new HTMLConfiguration());
    }

}