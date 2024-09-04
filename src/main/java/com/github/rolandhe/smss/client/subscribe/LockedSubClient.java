package com.github.rolandhe.smss.client.subscribe;

import com.github.rolandhe.smss.client.dlock.DLock;
import com.github.rolandhe.smss.client.dlock.EventWatcher;
import com.github.rolandhe.smss.client.dlock.LockEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class LockedSubClient implements Subscribe{
    private final SubConfig config;
    private final DLock dLock;
    private final String topicName;
    private final String who;
    private final long eventId;
    private final String key;

    public LockedSubClient(SubConfig config, DLock dLock, String topicName, String who, long eventId) {
        this.config = config;
        this.dLock = dLock;
        this.topicName = topicName;
        this.who = who;
        this.eventId = eventId;
        this.key = String.format("sub_lock@%s@%s", topicName,who);
    }

    @Override
    public void subscribe(SubMessageProcessor processor) {
        dLock.lockWatch(key, createWatcher(processor));
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
                if (event == LockEvent.LossLock) {
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
        Runnable func = new Runnable() {
            @Override
            public void run() {
                runWait.countDown();
                subCore(context, processor);
                release.waitEnd.countDown();
            }
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
                log.info("sub core exp", e);
                context.setClient(null);
                if(!context.waitNextTry()){
                    break;
                }
            }
        }
    }

    private static class SubContext {
        private SubClient client;
        private final AtomicBoolean stop = new AtomicBoolean(false);
        private final CountDownLatch couldEnd = new CountDownLatch(1);

        synchronized void setClient(SubClient client) {
            this.client = client;
        }

        boolean waitNextTry(){
            try {
                return !couldEnd.await(5 , TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.info("couldEnd await",e);
                return false;
            }
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
