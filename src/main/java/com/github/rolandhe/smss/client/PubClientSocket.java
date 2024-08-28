package com.github.rolandhe.smss.client;

import com.github.rolandhe.smss.client.bytes.BytesUtils;
import com.github.rolandhe.smss.client.msg.Message;
import com.github.rolandhe.smss.client.nets.SockUtil;
import com.github.rolandhe.smss.client.proto.ProtoConst;
import com.github.rolandhe.smss.client.proto.PubProto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
class PubClientSocket implements PubClient {
    private final String host;
    private final int port;
    private Socket sock;
    @Getter
    private boolean fetal;

    PubClientSocket(String host, int port) {
        this.host = host;
        this.port = port;
    }

    void init(int ioTimeout) throws IOException {
        this.sock = new Socket();
        sock.setSoTimeout(ioTimeout);
        sock.setKeepAlive(true);
        sock.setReuseAddress(true);
        sock.connect(new InetSocketAddress(host, port), ioTimeout);
    }

    @Override
    public void close() {
        if (sock != null) {
            try {
                sock.close();
            } catch (IOException e) {
                log.info("close socket error", e);
            }

            sock = null;
        }
    }

    @Override
    public boolean valid() {
        if (sock == null) {
            return false;
        }
        if (fetal) {
            return false;
        }
        return sock.isConnected() && alive();
    }

    @Override
    public OpResult publish(String topicName, Message message, String traceId) {
        byte[] content = PubProto.pocketPub(topicName, message, traceId);

        try {
            SockUtil.writeAll(sock, content);
            return readResult();
        } catch (IOException e) {
            this.fetal = true;
            log.info("publish {} error", topicName, e);
            return new OpResult(false, e.getMessage());
        }
    }

    @Override
    public OpResult publishDelay(String topicName, Message message, long delayMils, String traceId) {
        byte[] content = PubProto.pocketPubDelay(topicName, message, delayMils, traceId);

        try {
            SockUtil.writeAll(sock, content);
            return readResult();
        } catch (IOException e) {
            this.fetal = true;
            log.info("publishDelay {} error", topicName, e);
            return new OpResult(false, e.getMessage());
        }
    }

    @Override
    public OpResult createTopic(String topicName, long life, String traceId) {
        byte[] content = PubProto.pocketCreateTopic(topicName, life, traceId);

        try {
            SockUtil.writeAll(sock, content);
            return readResult();
        } catch (IOException e) {
            this.fetal = true;
            log.info("createTopic {} error", topicName, e);
            return new OpResult(false, e.getMessage());
        }
    }

    @Override
    public OpResult deleteTopic(String topicName, String traceId) {
        byte[] content = PubProto.pocketDeleteTopic(topicName, traceId);

        try {
            SockUtil.writeAll(sock, content);
            return readResult();
        } catch (IOException e) {
            this.fetal = true;
            log.info("deleteTopic {} error", topicName, e);
            return new OpResult(false, e.getMessage());
        }
    }

    @Override
    public TopicInfoResult getTopicInfo(String topicName, String traceId) {
        byte[] content = PubProto.pocketGetTopicInfo(topicName, traceId);

        try {
            SockUtil.writeAll(sock, content);
            TopicInfoResult ret = readMqList();
            return ret;
        } catch (IOException e) {
            this.fetal = true;
            log.info("getTopicInfo {} error", topicName, e);
            return new TopicInfoResult(false,e.getMessage());
        }
    }

    @Override
    public TopicInfoResult listTopicInfo(String traceId) {
        byte[] content = PubProto.pocketListTopic(traceId);

        try {
            SockUtil.writeAll(sock, content);
            TopicInfoResult ret = readMqList();
            return ret;
        } catch (IOException e) {
            this.fetal = true;
            log.info("listTopicInfo error", e);
            return new TopicInfoResult(false,e.getMessage());
        }
    }


    private boolean alive() {
        byte[] hbuf = new byte[ProtoConst.REQ_HEADER_SIZE];
        hbuf[0] = ProtoConst.CommandAlive;
        hbuf[19] = 0;
        try {
            SockUtil.writeAll(sock, hbuf);
            OpResult ret = readResult();
            return ret.isSuccess();
        } catch (IOException e) {
            this.fetal = true;
            log.info("alive error", e);
            return false;
        }

    }


    private OpResult readResult() throws IOException {
        byte[] hbuf = new byte[ProtoConst.RESP_HEADER_SIZE];
        readBuf(hbuf);
        short code = BytesUtils.getShortLittle(hbuf);
        if (code == ProtoConst.OkCode || code == ProtoConst.AliveCode) {
            return new OpResult(true, null);
        }
        short errLen = BytesUtils.getShortLittle(hbuf, 2);
        if (errLen == 0) {
            return new OpResult(false, "unknown error");
        }
        byte[] errMsgBuff = new byte[errLen];
        readBuf(errMsgBuff);
        return new OpResult(false, new String(errMsgBuff, StandardCharsets.UTF_8));
    }

    private void readBuf(byte[] buf) throws IOException{
        if (!SockUtil.readAll(sock, buf)) {
            this.fetal = true;
            throw new IOException("conn closed(read -1)");
        }
    }

    private TopicInfoResult readMqList() throws IOException {
        byte[] hbuf = new byte[ProtoConst.RESP_HEADER_SIZE];
        readBuf(hbuf);
        short code = BytesUtils.getShortLittle(hbuf);
        if (code == ProtoConst.OkCode) {
            int bodyLen = BytesUtils.getIntLittle(hbuf, 2);
            byte[] body = new byte[bodyLen];

            readBuf(body);
            return new TopicInfoResult(new String(body, StandardCharsets.UTF_8));
        }
        if (code != ProtoConst.ErrCode) {
            this.fetal = true;
            throw new RuntimeException("not support code");
        }
        short errLen = BytesUtils.getShortLittle(hbuf, 2);
        if (errLen == 0) {
            return new TopicInfoResult(false, "unknown error");
        }
        byte[] errMsgBuff = new byte[errLen];
        readBuf(errMsgBuff);
        return new TopicInfoResult(false, new String(errMsgBuff, StandardCharsets.UTF_8));
    }
}
