package com.github.rolandhe.smss.client.msg;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Header {
    private String name;
    private String value;

    @Override
    public String toString(){
        return String.format("%s:%s",name,value);
    }
}
