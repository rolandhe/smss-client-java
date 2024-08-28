package com.github.rolandhe.smss.client;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@Setter
@Getter
public class PoolConfig extends GenericObjectPoolConfig<PubClient> {
    public static final PoolConfig DefaultConfig;
    private int ioTimeout;

    static {
        PoolConfig config = new PoolConfig();
        config.setMaxTotal(15);
        config.setMaxIdle(10);
        config.setMinIdle(3);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.ioTimeout = 5 * 1000;
        DefaultConfig = config;
    }

    private PoolConfig() {

    }

    public PoolConfig(int maxTotal, int maxIdle, int minIdle) {
        setMaxTotal(maxTotal); // 最大连接数
        setMaxIdle(maxIdle);   // 最大空闲连接数
        setMinIdle(minIdle);
        setTestOnBorrow(true);
        setTestOnReturn(true);
        setTestWhileIdle(true);
    }
}
