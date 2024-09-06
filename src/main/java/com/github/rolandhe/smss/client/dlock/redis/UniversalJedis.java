package com.github.rolandhe.smss.client.dlock.redis;


import redis.clients.jedis.params.SetParams;

import java.io.IOException;
import java.util.List;

public interface UniversalJedis {
    Object eval(final String script, final List<String> keys, final List<String> args);

    String set(final String key, final String value, final SetParams params);
    String get(final String key);

    long del(String key);

    long expire(final String key, final long seconds);
    void close();
}
