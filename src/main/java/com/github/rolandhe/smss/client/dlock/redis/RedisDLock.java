package com.github.rolandhe.smss.client.dlock.redis;

import com.github.rolandhe.smss.client.dlock.DLock;
import com.github.rolandhe.smss.client.dlock.EventWatcher;
import com.github.rolandhe.smss.client.dlock.LockEvent;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Slf4j
public class RedisDLock implements DLock {
    private final String host;
    private final int port;
    private final boolean notSupportLua;
    private final boolean runInMain;

    public static final long LockedLife = 30L;
    public static final long TryLockTimeout = 10L;
    public static final long LeaseInterval = 25L;

    static final String LeaseScript = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then\n" +
            "        return redis.call(\"expire\", KEYS[1], ARGV[2])\n" +
            "    else\n" +
            "        return 0\n" +
            "    end";

    public RedisDLock(String host, int port, boolean notSupportLua,boolean runInMain) {
        this.host = host;
        this.port = port;
        this.notSupportLua = notSupportLua;
        this.runInMain = runInMain;
    }

    @Override
    public boolean lockWatch(String key, EventWatcher watcher) {
        String uid = UUID.randomUUID().toString();

        Runnable func = () -> {
            while (true) {
                Jedis jedis = null;
                try {
                    jedis = new Jedis(RedisDLock.this.host, RedisDLock.this.port);
                    loopLock(jedis, key, uid, watcher);
                } catch (RuntimeException e) {
                    log.info("lock watch while error", e);
                } finally {
                    if (jedis != null) {
                        jedis.close();
                    }
                }
            }
        };
        if(runInMain){
            func.run();
        }else {
            Thread t = new Thread(func,"lockWatch-thread");
            t.start();
        }


        return true;
    }



    private void loopLock(Jedis jedis, String key, String uid, EventWatcher watcher) {
        int stateMachine = 0;
        while (true) {
            if (stateMachine == 0) {
                SetParams params = new SetParams();
                params.nx().ex(LockedLife);
                String ret = jedis.set(key, uid, params);
                long timeout = TryLockTimeout;
                if (ret == null) {
                    watcher.watch(LockEvent.LockTimeout);
                } else {
                    watcher.watch(LockEvent.Locked);
                    timeout = LeaseInterval;
                    stateMachine = 1;
                }
                safeSleep(timeout);
                continue;
            }
            if (stateMachine == 1) {
                long timeout = TryLockTimeout;
                if (!lease(jedis, key, uid)) {
                    watcher.watch(LockEvent.LossLock);
                    stateMachine = 0;
                } else {
                    watcher.watch(LockEvent.Leased);
                    timeout = LeaseInterval;
                }
                safeSleep(timeout);
            }
        }

    }

    private void safeSleep(long timeout) {
        try {
            Thread.sleep(timeout * 1000);
        } catch (InterruptedException e) {
            log.info("sleep met exp", e);
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
