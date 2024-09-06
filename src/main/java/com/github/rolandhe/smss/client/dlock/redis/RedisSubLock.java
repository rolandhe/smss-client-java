package com.github.rolandhe.smss.client.dlock.redis;

import com.github.rolandhe.smss.client.dlock.SubLock;
import com.github.rolandhe.smss.client.dlock.EventWatcher;
import com.github.rolandhe.smss.client.dlock.LockEvent;
import com.github.rolandhe.smss.client.dlock.QuickWaiter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于redis或者类redis实现的分布式锁。
 * 由于redis锁不像zookeeper那样可以持续不断的监控，使用redis需要使用轮询技术来模拟连续性。
 *
 */
@Slf4j
public class RedisSubLock implements SubLock {
    private final String host;
    private final int port;
    private final boolean notSupportLua;
    private final boolean runInMain;

    private final AtomicBoolean shutdownState = new AtomicBoolean(false);
    private final AtomicBoolean runningState = new AtomicBoolean(false);
    private final CountDownLatch waitShutdownComplete = new CountDownLatch(1);

    private final QuickWaiter quickWaiter = new QuickWaiter();

    public static final long LockedLife = 30L;
    public static final long TryLockTimeout = 10L;
    public static final long LeaseInterval = 25L;

    static final String LeaseScript = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then\n" +
            "        return redis.call(\"expire\", KEYS[1], ARGV[2])\n" +
            "    else\n" +
            "        return 0\n" +
            "    end";
    static final String ReleaseScript = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then\n" +
            "            return redis.call(\"del\", KEYS[1])\n" +
            "        else\n" +
            "            return 0\n" +
            "        end";


    private RedisSubLock(String host, int port, boolean notSupportLua, boolean runInMain) {
        this.host = host;
        this.port = port;
        this.notSupportLua = notSupportLua;
        this.runInMain = runInMain;
    }

    /**
     * 创建生产环境中的redis 锁，锁的监控在独立的线程中运行
     *
     * @param host redis host
     * @param port redis port
     * @param notSupportLua 不支持lua，类redis产品，比如pika，不支持lua
     * @return
     */
    public static SubLock factory(String host, int port, boolean notSupportLua){
        return new RedisSubLock(host,port,notSupportLua,false);
    }

    /**
     * 创建测试环境中的redis 锁，锁的监控在当前主线程中运行
     *
     * @param host
     * @param port
     * @param notSupportLua
     * @return
     */
    public static SubLock factoryInMainThread(String host, int port, boolean notSupportLua){
        return new RedisSubLock(host,port,notSupportLua,true);
    }

    @Override
    public boolean lockWatch(String key, EventWatcher watcher) {
        String uid = UUID.randomUUID().toString();

        CountDownLatch started = new CountDownLatch(1);
        Runnable func = () -> {
            runningState.set(true);
            started.countDown();
            Jedis jedis = new Jedis(RedisSubLock.this.host, RedisSubLock.this.port);
            Recorder recorder = new Recorder(notSupportLua);
            while (!shutdownState.get()) {
                try  {
                    loopLock(recorder,jedis, key, uid, watcher);
                } catch (RuntimeException e) {
                    log.info("lock watch while error", e);
                }
            }

            if(!runInMain){
                watcher.watch(LockEvent.LockerShutdown);
            }
            RedisSubLock.this.releaseLock(key,uid,jedis,recorder.canRemove());
            jedis.close();
            log.info("loop lock thread end, release redis and lock resource");
            waitShutdownComplete.countDown();
        };
        if(runInMain){
            func.run();
        }else {
            Thread t = new Thread(func,"lockWatch-thread");
            t.start();
            try {
                started.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    @Override
    public void shutdown(){
        shutdownState.set(true);
        quickWaiter.countDown();
        if(runInMain||!runningState.get()){
            log.info("not start thread,exit");
            return;
        }
        try {
            waitShutdownComplete.await();
        } catch (InterruptedException e) {
            log.info("waitShutdownComplete await",e);
        }
        log.info("shutdown all threads");
    }

    private boolean releaseLock(String key,String value,Jedis jedis,boolean canRemove){
        if(this.notSupportLua){
            if(!canRemove){
                return false;
            }
            long ret = jedis.del(key);
            log.info("releaseLock del,ret={}",ret);
            return ret == 1;
        }
        Object ret = jedis.eval(ReleaseScript,Collections.singletonList(key),Collections.singletonList(value));
        log.info("releaseLock ReleaseScript,ret={}",ret);
        return (long) ret == 1L;
    }


    private static class Recorder{
        long expireAt;
        final boolean notLua;

        private Recorder(boolean notLua) {
            this.notLua = notLua;
        }
        void record(){
            if(notLua){
                expireAt = System.currentTimeMillis() + LockedLife * 1000L;
            }
        }
        void reset(){
            if(notLua){
                expireAt = 0L;
            }
        }
        boolean canRemove(){
            if(!notLua){
                return false;
            }
            return expireAt - System.currentTimeMillis() > 5 * 1000L;
        }
    }

    private void loopLock(Recorder recorder,Jedis jedis, String key, String uid, EventWatcher watcher) {
        int stateMachine = 0;
        while (!shutdownState.get()) {
            if (stateMachine == 0) {
                SetParams params = new SetParams();
                params.nx().ex(LockedLife);
                recorder.record();
                String ret = jedis.set(key, uid, params);
                long timeout = TryLockTimeout;
                if (ret == null) {
                    watcher.watch(LockEvent.LockTimeout);
                    recorder.reset();
                } else {
                    watcher.watch(LockEvent.Locked);
                    timeout = LeaseInterval;
                    stateMachine = 1;
                }

                quickWaiter.await(timeout, TimeUnit.SECONDS);

                continue;
            }
            if (stateMachine == 1) {
                long timeout = TryLockTimeout;
                recorder.record();
                if (!lease(jedis, key, uid)) {
                    recorder.reset();
                    watcher.watch(LockEvent.LossLock);
                    stateMachine = 0;
                } else {
                    watcher.watch(LockEvent.Leased);
                    timeout = LeaseInterval;
                }
                quickWaiter.await(timeout, TimeUnit.SECONDS);
            }
        }

    }


    private boolean lease(Jedis jedis, String key, String uid) {
        if (this.notSupportLua) {
            String v = jedis.get(key);
            if(!uid.equals(v)){
                return false;
            }
            long ret = jedis.expire(key, LockedLife);
            return ret == 1L;
        }
        List<String> keys = Collections.singletonList(key);
        List<String> values = Arrays.asList(uid, LockedLife + "");
        Object ret = jedis.eval(LeaseScript, keys, values);
        return (long) ret == 1L;
    }
}
