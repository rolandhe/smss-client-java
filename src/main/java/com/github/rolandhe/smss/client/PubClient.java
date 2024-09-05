package com.github.rolandhe.smss.client;

import com.github.rolandhe.smss.client.msg.Message;


/**
 * 消息发布及topic管理客户端
 */
public interface PubClient {
    void close();

    /**
     * 探活当前连接是否有效
     *
     * @return
     */
    boolean valid();

    /**
     * 发布消息
     *
     * @param topicName topic name
     * @param message 消息
     * @param traceId 用于日志记录的id，可以为空，可以使用uuid
     * @return 成功与否结果
     */
    OpResult publish(String topicName, Message message, String traceId);

    /**
     * 发送延迟消息，基本同 publish
     *
     * @param topicName
     * @param message
     * @param delayMils 延迟多少毫秒
     * @param traceId
     * @return
     */
    OpResult publishDelay(String topicName, Message message, long delayMils, String traceId);

    /**
     * 创建topic，可以指定topic的生命周期，如果生命到期，topic会自动被清除
     *
     * @param topicName
     * @param life   topic生命截止日期，毫秒时间戳
     * @param traceId 用于日志记录的id，可以为空，可以使用uuid
     * @return
     */
    OpResult createTopic(String topicName, long life, String traceId);

    /**
     * 删除topic
     *
     * @param topicName
     * @param traceId
     * @return
     */
    OpResult deleteTopic(String topicName, String traceId);

    /**
     * 读取单个topic的信息，返回信息是json
     *
     * @param topicName
     * @param traceId
     * @return json，描述topic信息
     */
    TopicInfoResult getTopicInfo(String topicName,String traceId);

    /**
     * 获取所有topic的信息，返回json
     *
     * @param traceId
     * @return json，以数组形式描述多个topic
     */
    TopicInfoResult listTopicInfo(String traceId);
}
