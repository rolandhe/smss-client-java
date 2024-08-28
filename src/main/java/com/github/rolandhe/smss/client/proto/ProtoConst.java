package com.github.rolandhe.smss.client.proto;

public interface ProtoConst {
    int REQ_HEADER_SIZE = 20;
    int RESP_HEADER_SIZE = 10;


    byte CommandSub    = 0;
    byte CommandPub          = 1;
    byte CommandCreateTopic  = 2;
    byte CommandDeleteTopic  = 3;

    byte CommandDelay      = 16;
    byte CommandAlive      = 17;
    byte CommandReplica    = 64;
    byte CommandTopicInfo  = 65;
    byte CommandList       = 100;



    short OkCode     = 200;
    short AliveCode  = 201;
    short SubEndCode = 255;
    short ErrCode    = 400;
}
