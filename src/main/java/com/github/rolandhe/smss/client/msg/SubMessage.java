package com.github.rolandhe.smss.client.msg;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class SubMessage {
    private Map<String,Header> headerMap = new LinkedHashMap<>();
    private byte[] payload;
    private long ts;
    private long eventId;
    private long fileId;
    private long filePos;

    public void addHeader(String name,String value){
        Header h = headerMap.get(name);
        if (h != null){
            h.setValue(value);
            return;
        }
        h = new Header(name,value);
        headerMap.put(name,h);
    }
}
