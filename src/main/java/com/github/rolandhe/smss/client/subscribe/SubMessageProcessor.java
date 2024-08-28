package com.github.rolandhe.smss.client.subscribe;

import com.github.rolandhe.smss.client.msg.SubMessage;

import java.util.List;

public interface SubMessageProcessor {
    MsgProcResult process(List<SubMessage> message);
}
