package com.github.rolandhe.smss.client;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

class PubClientFactory implements PooledObjectFactory<PubClient> {
    private final String host;
    private final int port;
    private final int ioTimeout;

    PubClientFactory(String host, int port,int ioTimeout) {
        this.host = host;
        this.port = port;
        this.ioTimeout = ioTimeout;
    }

    @Override
    public void activateObject(PooledObject<PubClient> p) throws Exception {

    }

    @Override
    public void destroyObject(PooledObject<PubClient> p) throws Exception {
        p.getObject().close();
    }


    @Override
    public PooledObject<PubClient> makeObject() throws Exception {
        PubClientSocket ps = new PubClientSocket(host,port);
        ps.init(this.ioTimeout);
        return new DefaultPooledObject<>(ps);
    }

    @Override
    public void passivateObject(PooledObject<PubClient> p) throws Exception {

    }

    @Override
    public boolean validateObject(PooledObject<PubClient> p) {

        return p.getObject().valid();
    }
}
