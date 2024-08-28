package com.github.rolandhe.smss.client.proto;

import com.github.rolandhe.smss.client.bytes.BytesUtils;
import com.github.rolandhe.smss.client.msg.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;


public class PubProto {
    public static byte[] pocketPub(String topicName, Message message, String traceId){
        byte[] payload = message.toBytes();
        if(payload == null || payload.length <= 8){
            throw new RuntimeException("empty message");
        }
        byte[] holder = new byte[ProtoConst.REQ_HEADER_SIZE];
        holder[0] = ProtoConst.CommandPub;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(payload.length + 256);
        try {
            bos.write(holder);

            byte[] topicBuf = topicName.getBytes(StandardCharsets.UTF_8);
            BytesUtils.putShortLittle(holder,1,topicBuf.length);
            bos.write(topicBuf);

            if(traceId == null || traceId.isEmpty()){
                holder[19] = 0;
            } else  {
                byte[] tidBuff = traceId.getBytes(StandardCharsets.UTF_8);
                holder[19] = (byte)tidBuff.length;
                bos.write(tidBuff);
            }

            BytesUtils.putIntLittle(holder,3,payload.length);
            bos.write(payload);

            byte[] content = bos.toByteArray();
            System.arraycopy(holder,0,content,0,ProtoConst.REQ_HEADER_SIZE);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] pocketPubDelay(String topicName, Message message,long delay, String traceId){
        byte[] payload = message.toBytes();
        if(payload == null || payload.length <= 8){
            throw new RuntimeException("empty message");
        }
        byte[] holder = new byte[ProtoConst.REQ_HEADER_SIZE];
        holder[0] = ProtoConst.CommandDelay;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(payload.length + 256);
        try {
            bos.write(holder);

            byte[] topicBuf = topicName.getBytes(StandardCharsets.UTF_8);
            BytesUtils.putShortLittle(holder,1,topicBuf.length);
            bos.write(topicBuf);

            if(traceId == null || traceId.isEmpty()){
                holder[19] = 0;
            } else  {
                byte[] tidBuff = traceId.getBytes(StandardCharsets.UTF_8);
                holder[19] = (byte)tidBuff.length;
                bos.write(tidBuff);
            }


            bos.write(BytesUtils.long2BytesLittle(delay));

            BytesUtils.putIntLittle(holder,3,payload.length);
            bos.write(payload);

            byte[] content = bos.toByteArray();
            System.arraycopy(holder,0,content,0,ProtoConst.REQ_HEADER_SIZE);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] pocketCreateTopic(String topicName, long life, String traceId) {
        byte[] holder = new byte[ProtoConst.REQ_HEADER_SIZE];
        holder[0] = ProtoConst.CommandCreateTopic;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
        try {
            bos.write(holder);

            byte[] topicBuf = topicName.getBytes(StandardCharsets.UTF_8);
            BytesUtils.putShortLittle(holder, 1, topicBuf.length);
            bos.write(topicBuf);

            if (traceId == null || traceId.isEmpty()) {
                holder[19] = 0;
            } else {
                byte[] tidBuff = traceId.getBytes(StandardCharsets.UTF_8);
                holder[19] = (byte) tidBuff.length;
                bos.write(tidBuff);
            }


            bos.write(BytesUtils.long2BytesLittle(life));

            byte[] content = bos.toByteArray();
            System.arraycopy(holder, 0, content, 0, ProtoConst.REQ_HEADER_SIZE);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] pocketDeleteTopic(String topicName, String traceId) {
        return pocketSimpleTopic(ProtoConst.CommandDeleteTopic,topicName,traceId);
    }

    public static byte[] pocketGetTopicInfo(String topicName, String traceId) {
        return pocketSimpleTopic(ProtoConst.CommandTopicInfo,topicName,traceId);
    }

    private static byte[] pocketSimpleTopic(byte cmd,String topicName, String traceId) {
        byte[] holder = new byte[ProtoConst.REQ_HEADER_SIZE];
        holder[0] = cmd;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
        try {
            bos.write(holder);

            byte[] topicBuf = topicName.getBytes(StandardCharsets.UTF_8);
            BytesUtils.putShortLittle(holder, 1, topicBuf.length);
            bos.write(topicBuf);

            if (traceId == null || traceId.isEmpty()) {
                holder[19] = 0;
            } else {
                byte[] tidBuff = traceId.getBytes(StandardCharsets.UTF_8);
                holder[19] = (byte) tidBuff.length;
                bos.write(tidBuff);
            }


            byte[] content = bos.toByteArray();
            System.arraycopy(holder, 0, content, 0, ProtoConst.REQ_HEADER_SIZE);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] pocketListTopic(String traceId) {
        byte[] holder = new byte[ProtoConst.REQ_HEADER_SIZE];
        holder[0] = ProtoConst.CommandList;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(128);
        try {
            bos.write(holder);

            if (traceId == null || traceId.isEmpty()) {
                holder[19] = 0;
            } else {
                byte[] tidBuff = traceId.getBytes(StandardCharsets.UTF_8);
                holder[19] = (byte) tidBuff.length;
                bos.write(tidBuff);
            }

            byte[] content = bos.toByteArray();
            System.arraycopy(holder, 0, content, 0, ProtoConst.REQ_HEADER_SIZE);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
