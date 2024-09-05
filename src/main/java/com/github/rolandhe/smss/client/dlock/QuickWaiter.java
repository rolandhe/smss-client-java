package com.github.rolandhe.smss.client.dlock;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class QuickWaiter {
    private final CountDownLatch waiter = new CountDownLatch(1);
    public boolean await(long timeout, TimeUnit timeUnit){
        try {
           return waiter.await(timeout,timeUnit);
        } catch (InterruptedException e) {
            log.info("QuickWaiter await exp",e);
        }
        return false;
    }
    public void countDown(){
        waiter.countDown();
    }
}
