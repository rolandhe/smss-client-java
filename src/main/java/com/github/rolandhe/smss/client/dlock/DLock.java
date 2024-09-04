package com.github.rolandhe.smss.client.dlock;

public interface DLock {
    boolean lockWatch(String key,EventWatcher watcher);
}
