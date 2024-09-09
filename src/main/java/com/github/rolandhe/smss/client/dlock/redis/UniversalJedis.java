package com.github.rolandhe.smss.client.dlock.redis;


import redis.clients.jedis.params.SetParams;

import java.util.List;

/**
 * 抽象使用redis实现锁的操作
 *
 */
public interface UniversalJedis {
    /**
     * 执行lua脚本
     *
     * @param script
     * @param keys
     * @param args
     * @return
     */
    Object eval(final String script, final List<String> keys, final List<String> args);

    /**
     * 实现set
     *
     * @param key
     * @param value
     * @param params
     * @return
     */
    String set(final String key, final String value, final SetParams params);

    /**
     * 实现get
     *
     * @param key
     * @return
     */
    String get(final String key);

    /**
     * 实现del
     *
     * @param key
     * @return
     */
    long del(String key);

    /**
     * 过期
     *
     * @param key
     * @param seconds
     * @return
     */
    long expire(final String key, final long seconds);

    /**
     * 关闭连接
     */
    void close();
}
