package com.github.rolandhe.smss.client.subscribe;

import com.github.rolandhe.smss.client.dlock.SubLock;
import com.github.rolandhe.smss.client.dlock.EventWatcher;
import com.github.rolandhe.smss.client.dlock.LockEvent;
import com.github.rolandhe.smss.client.dlock.QuickWaiter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 支持分布式锁的订阅者实现，一般用于生产环境中, 该实现保证了在多活环境下，同一个业务只有一个实例能够消费同一个topic
 *
 */
@Slf4j
public class LockedSubClient implements Subscribe{
    private final SubConfig config;
    private final SubLock subLock;
    private final String topicName;
    private final String who;
    private final long eventId;
    private final String key;

    /**
     *
     * @param config
     * @param subLock
     * @param topicName
     * @param who     消费者是谁，类kafka的group，表示某个业务
     * @param eventId
     */
    public LockedSubClient(SubConfig config, SubLock subLock, String topicName, String who, long eventId) {
        this.config = config;
        this.subLock = subLock;
        this.topicName = topicName;
        this.who = who;
        this.eventId = eventId;
        this.key = String.format("sub_lock@%s@%s", topicName,who);
    }

    @Override
    public void subscribe(SubMessageProcessor processor) {
        subLock.lockWatch(key, createWatcher(processor));
    }

    private static abstract class ClearRelease {
        private final CountDownLatch waitEnd = new CountDownLatch(1);

        abstract void shutdown();

        void waitEnd() {
            try {
                waitEnd.await();
            } catch (InterruptedException e) {
                log.info("wait end exp", e);
            }
        }
    }

    private EventWatcher createWatcher(SubMessageProcessor processor) {
        return new EventWatcher() {
            ClearRelease release = null;

            @Override
            public void watch(LockEvent event) {
                log.info("watch event {}", event);
                if (event == LockEvent.Locked) {
                    release = start(processor);
                    return;
                }
                if (event == LockEvent.LossLock || event == LockEvent.LockerShutdown) {
                    if (release != null) {
                        release.shutdown();
                        release.waitEnd();
                        release = null;
                    }
                }
            }
        };
    }

    private ClearRelease start(SubMessageProcessor processor) {
        SubContext context = new SubContext();
        ClearRelease release = new ClearRelease() {
            @Override
            void shutdown() {
                context.endNotify();
            }
        };
        CountDownLatch runWait = new CountDownLatch(1);
        Runnable func = () -> {
            runWait.countDown();
            subCore(context, processor);
            log.info("sub-core end");
            release.waitEnd.countDown();
        };

        Thread t = new Thread(func, "sub-core");
        t.start();
        try {
            runWait.await();
        } catch (InterruptedException e) {
            log.info("wait run sub thread error", e);
        }

        log.info("start to subscribe");

        return release;
    }

    private void subCore(SubContext context, SubMessageProcessor processor) {
        while(!context.stop.get()) {
            try {
                SubClient client = new SubClient(config, topicName, who, eventId);
                context.setClient(client);
                client.subscribe(processor);
            } catch (RuntimeException e) {
                if(context.stop.get() && checkSocketCloseExp(e)){
                    log.info("socket closed by lock shutdown");
                }else {
                    log.info("sub core exp", e);
                }
                context.setClient(null);
                if(!context.waitNextTry()){
                    break;
                }
            }
        }
    }

    private boolean checkSocketCloseExp(RuntimeException e){
        if(e.getCause() instanceof SocketException){
            SocketException se = (SocketException) e.getCause();
            return "Socket closed".equals(se.getMessage());
        }
        return false;
    }

    private static class SubContext {
        private SubClient client;
        private final AtomicBoolean stop = new AtomicBoolean(false);
        private final QuickWaiter couldEnd = new QuickWaiter();

        synchronized void setClient(SubClient client) {
            this.client = client;
        }

        boolean waitNextTry(){
           return couldEnd.await(5 , TimeUnit.SECONDS);
        }

        synchronized void endNotify() {
            stop.set(true);
            couldEnd.countDown();
            if (client == null) {
                return;
            }

            client.shutDown();
            client = null;
        }
    }

}
