package com.github.rolandhe.smss.client.dlock.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.commands.KeyCommands;
import redis.clients.jedis.commands.ScriptingKeyCommands;
import redis.clients.jedis.commands.StringCommands;
import redis.clients.jedis.params.SetParams;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

@Slf4j
public class DefaultUniversalJedis implements UniversalJedis{
    private final Object rawObject;

    public DefaultUniversalJedis(Object rawObject) {
        this.rawObject = rawObject;
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        if(rawObject instanceof ScriptingKeyCommands){
            ScriptingKeyCommands cmd = (ScriptingKeyCommands)rawObject;
            return cmd.eval(script,keys,args);
        }
        throw new RuntimeException("not support ScriptingKeyCommands");
    }

    @Override
    public String set(String key, String value, SetParams params) {
        if(rawObject instanceof StringCommands){
            StringCommands cmd = (StringCommands)rawObject;
            return cmd.set(key,value,params);
        }
        throw new RuntimeException("not support StringCommands");
    }

    @Override
    public String get(String key) {
        if(rawObject instanceof StringCommands){
            StringCommands cmd = (StringCommands)rawObject;
            return cmd.get(key);
        }
        throw new RuntimeException("not support StringCommands");
    }

    @Override
    public long del(String key) {
        if(rawObject instanceof KeyCommands){
            KeyCommands cmd = (KeyCommands)rawObject;
            return cmd.del(key);
        }
        throw new RuntimeException("not support KeyCommands");
    }

    @Override
    public long expire(String key, long seconds) {
        if(rawObject instanceof KeyCommands){
            KeyCommands cmd = (KeyCommands)rawObject;
            return cmd.expire(key,seconds);
        }
        throw new RuntimeException("not support KeyCommands");
    }

    @Override
    public void close()  {
        if(rawObject instanceof Closeable){
            Closeable cmd = (Closeable)rawObject;
            try {
                cmd.close();
            } catch (IOException e) {
                // ignore
                log.info("call DefaultUniversalJedis close exp",e);
            }
            return;
        }
        throw new RuntimeException("not support Closeable");
    }
}
