package com.github.rolandhe.smss.client.dlock;

/**
 * 分布式锁，用于协调多个实例的同一个业务消费同一个topic
 */
public interface SubLock {
    /**
     * 锁定监控
     *
     * @param key
     * @param watcher 监控事件回调
     * @return
     */
    boolean lockWatch(String key,EventWatcher watcher);
    void shutdown();
}
