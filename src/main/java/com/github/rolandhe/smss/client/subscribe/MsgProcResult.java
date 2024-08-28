package com.github.rolandhe.smss.client.subscribe;

import lombok.Getter;

@Getter
public enum MsgProcResult {
    ACK(0),
    AckWithEnd(1),
    ClientTermiteWithoutAck(2),
    ;
    private final int code;

    MsgProcResult(int code) {
        this.code = code;
    }
}
