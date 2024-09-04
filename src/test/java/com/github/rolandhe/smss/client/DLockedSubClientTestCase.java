package com.github.rolandhe.smss.client;

import com.github.rolandhe.smss.client.dlock.DLock;
import com.github.rolandhe.smss.client.dlock.redis.RedisDLock;
import com.github.rolandhe.smss.client.msg.Header;
import com.github.rolandhe.smss.client.msg.SubMessage;
import com.github.rolandhe.smss.client.subscribe.LockedSubClient;
import com.github.rolandhe.smss.client.subscribe.MsgProcResult;
import com.github.rolandhe.smss.client.subscribe.SubClient;
import com.github.rolandhe.smss.client.subscribe.SubConfig;
import com.github.rolandhe.smss.client.subscribe.SubMessageProcessor;
import com.github.rolandhe.smss.client.subscribe.Subscribe;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;

@Slf4j
public class DLockedSubClientTestCase {
    @Test
    public void testSub(){
        DLock lock = new RedisDLock("localhost",6379,true,true);

        Subscribe lockedSubClient = new LockedSubClient(SubConfig.newDefault("localhost",12301),lock,"order", "vvi", 0);

        lockedSubClient.subscribe( new SubMessageProcessor() {
            @Override
            public MsgProcResult process(List<SubMessage> messageList) {
                for(SubMessage msg : messageList){
                    StringBuilder sb = new StringBuilder();
                    if(!msg.getHeaderMap().isEmpty()){
                        for(Entry<String, Header> h : msg.getHeaderMap().entrySet()){
                            sb.append(h.getValue().toString()).append(",");
                        }
                        sb.delete(sb.length() - 1,sb.length());
                    }
                    log.info("msg, eventId={},header={},payload={}",msg.getEventId(),sb.toString(),new String(msg.getPayload(), StandardCharsets.UTF_8));
                }
                return MsgProcResult.ACK;
            }
        });
    }
}
