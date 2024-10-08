package com.github.rolandhe.smss.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * PubClient池，池化的PubClient可以被服用
 *
 */
@Slf4j
public class PubClientPool {
    private final GenericObjectPool<PubClient> pool;

    public PubClientPool(String host, int port) {
        this(PoolConfig.DefaultConfig,host,port);
    }

    public PubClientPool(PoolConfig config,String host, int port) {
        PubClientFactory factory = new PubClientFactory(host, port,config.getIoTimeout());
        pool = new GenericObjectPool<>(factory, config);
    }

    /**
     * 从池中借用一个PubClient
     *
     * @return
     */
    public PubClient borrow()  {
        try {
            PubClient real =  pool.borrowObject();
            if(real == null){
                return null;
            }
            return new PooledPubClient(real,this);
        }catch (Exception e){
            log.info("borrow client failed.",e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 释放池
     */
    public void shutDown() {
        pool.close();
    }

    void returnClient(PubClient pc,boolean release) throws Exception {
        if(release){
            pool.invalidateObject(pc);
            return;
        }
        pool.returnObject(pc);
    }
}
