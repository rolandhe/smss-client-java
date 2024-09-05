package com.github.rolandhe.smss.client.subscribe;

import lombok.Getter;
import lombok.Setter;

/**
 * 订阅者配置
 */
@Getter
@Setter
public class SubConfig {
    private String host;
    private int port;

    /**
     * socket的so timeout
     */
    private int soTimeout;

    /**
     * 订阅者客户端允许没有任何消息的最大时间，如果超过这个最大时间，客户端就认为smss服务端已经断掉，
     * 客户端会关闭当前链接，并重新连接
     */
    private int maxNoDataTimeout;
    /**
     * 批量订阅，每次可以从smss server读取batchSize条消息
     */
    private short batchSize;
    /**
     * 订阅者处理消息所花的最大时间，如果smss server在ackTimeout时间内没有收到订阅端的ack，smss server就认为
     * 订阅者socket已经断联，smss server就会关闭socket并释放所有资源
     */
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
