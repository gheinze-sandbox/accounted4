package com.accounted4.midtier.service;

import lombok.Data;

/**
 * Simple bean to support sending a value back to the client
 * as a json string with auto-marshalling.
 * 
 * @author glenn
 */
@Data
public class IdBean {

    private final String id;
 
    public IdBean(String id) {
        this.id = id;
    }
    
}
