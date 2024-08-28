package com.github.rolandhe.smss.client;

import com.github.rolandhe.smss.client.msg.Message;



public interface PubClient {
    void close();
    boolean valid();

    OpResult publish(String topicName, Message message, String traceId);

    OpResult publishDelay(String topicName, Message message, long delayMils, String traceId);

    OpResult createTopic(String topicName, long life, String traceId);

    OpResult deleteTopic(String topicName, String traceId);

    TopicInfoResult getTopicInfo(String topicName,String traceId);

    TopicInfoResult listTopicInfo(String traceId);
}
