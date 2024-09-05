package com.github.rolandhe.smss.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class OpResult {
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 出错描述信息，如果 success==true，errMessage=null
     */
    private String errMessage;
}
