package com.github.rolandhe.smss.client.dlock;

public interface SubLock {
    boolean lockWatch(String key,EventWatcher watcher);
    void shutdown();
}
