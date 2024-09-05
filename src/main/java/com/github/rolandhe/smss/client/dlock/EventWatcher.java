package com.github.rolandhe.smss.client.dlock;

/**
 * 锁事件的监听器
 */
public interface EventWatcher {
    /**
     * 监听锁事件
     * 
     * @param event
     */
    void watch(LockEvent event);
}
