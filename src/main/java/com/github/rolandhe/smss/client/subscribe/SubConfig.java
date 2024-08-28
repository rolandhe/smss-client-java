package com.github.rolandhe.smss.client.subscribe;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubConfig {
    private String host;
    private int port;
    private int soTimeout;
    private int maxNoDataTimeout;
    private short batchSize;
    private long ackTimeout;

    public static SubConfig newDefault(String host,int port){
        SubConfig config = new SubConfig();
        config.host = host;
        config.port = port;
        config.soTimeout = 5*1000;
        config.maxNoDataTimeout = 35 * 1000;
        config.batchSize = 5;
        config.ackTimeout = 3 * 1000;
        return config;
    }
}
