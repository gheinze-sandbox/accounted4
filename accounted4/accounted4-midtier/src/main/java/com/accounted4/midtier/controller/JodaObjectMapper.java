package com.accounted4.midtier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.annotation.PostConstruct;

/**
 * Register the JodaModule with Jackson in order to support automatic
 * marshalling/un-marshalling of JodaTime objects.
 * 
 * @author glenn
 */
public class JodaObjectMapper extends ObjectMapper {
    
    @PostConstruct
    public void init() {
        registerModule(new JodaModule());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        setDateFormat(dateFormat);
    }
    
}
