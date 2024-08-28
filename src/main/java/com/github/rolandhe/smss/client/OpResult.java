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
    private boolean success;
    private String errMessage;
}
