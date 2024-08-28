package com.github.rolandhe.smss.client;

import com.github.rolandhe.smss.client.msg.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class PooledPubClient implements PubClient{
    private final PubClient real;
    private final PubClientPool pool;

    PooledPubClient(PubClient real, PubClientPool pool) {
        this.real = real;
        this.pool = pool;
    }

    @Override
    public void close()  {
        boolean release = false;
        if(real instanceof PubClientSocket){
            PubClientSocket psock = (PubClientSocket) real;
            release = psock.isFetal();
        }
        try {
            pool.returnClient(real,release);
        } catch (Exception e) {
            log.info("return pub client exp",e);
        }
    }

    @Override
    public boolean valid() {
        return real.valid();
    }

    @Override
    public OpResult publish(String topicName, Message message, String traceId) {
        return real.publish(topicName,message,traceId);
    }

    @Override
    public OpResult publishDelay(String topicName, Message message, long delayMils, String traceId) {
        return real.publishDelay(topicName,message,delayMils,traceId);
    }

    @Override
    public OpResult createTopic(String topicName, long life, String traceId) {
        return real.createTopic(topicName,life,traceId);
    }

    @Override
    public OpResult deleteTopic(String topicName, String traceId) {
        return real.deleteTopic(topicName,traceId);
    }

    @Override
    public TopicInfoResult getTopicInfo(String topicName, String traceId) {
        return real.getTopicInfo(topicName,traceId);
    }

    @Override
    public TopicInfoResult listTopicInfo(String traceId) {
        return real.listTopicInfo(traceId);
    }
}
