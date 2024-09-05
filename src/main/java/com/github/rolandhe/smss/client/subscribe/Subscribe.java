package com.github.rolandhe.smss.client.subscribe;

/**
 * 描述消息订阅者
 *
 */
public interface Subscribe {
    /**
     * 持续不断地订阅消息
     *
     * @param processor  消息处理回调
     */
    void subscribe(SubMessageProcessor processor);
}
