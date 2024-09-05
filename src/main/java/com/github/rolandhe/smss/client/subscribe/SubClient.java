package com.github.rolandhe.smss.client.subscribe;

import com.github.rolandhe.smss.client.bytes.BytesUtils;
import com.github.rolandhe.smss.client.msg.SubMessage;
import com.github.rolandhe.smss.client.nets.SockUtil;
import com.github.rolandhe.smss.client.proto.ProtoConst;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单环境的消息订阅者实现，用于只有单个实例订阅消息的场景，在多活、微服务场景下一般使用 LockedSubClient
 */
@Slf4j
public class SubClient implements Subscribe {
    private final SubConfig config;
    private final Socket sock = new Socket();
    private final AtomicInteger state = new AtomicInteger(0);
    private final String topicName;
    private final String who;
    private final long eventId;


    public enum RunningState{
        Init(0),
        Running(1),
        Complete(2),
        ;

        private final int value;

        RunningState(int value) {
            this.value = value;
        }
        public static RunningState of(int value){
            for(RunningState s : RunningState.values()){
                if(value ==s.value){
                    return s;
                }
            }
            return null;
        }
    }

    /**
     *
     *
     * @param config
     * @param topicName
     * @param who
     * @param eventId
     */
    public SubClient(SubConfig config, String topicName, String who, long eventId) {
        this.config = config;
        this.topicName = topicName;
        this.who = who;
        this.eventId = eventId;
    }


    @Override
    public void subscribe(SubMessageProcessor processor) {
        if(state.get() != RunningState.Init.value){
            throw new RuntimeException("invalid SubClient");
        }
        try {
            sock.setSoTimeout(config.getSoTimeout());
            sock.setKeepAlive(true);
            sock.setReuseAddress(true);
            sock.connect(new InetSocketAddress(config.getHost(), config.getPort()), config.getSoTimeout());
            state.set(RunningState.Running.value);
            byte[] cmdBuf = pocketSubCmd(topicName, who, eventId);
            SockUtil.writeAll(sock, cmdBuf);
            waitMessage(processor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            releaseSocket();
        }
    }

    private void waitMessage(SubMessageProcessor processor) throws IOException {
        byte[] hbuf = new byte[ProtoConst.RESP_HEADER_SIZE];
        byte[] ack = new byte[2];
        long lastTime = System.currentTimeMillis();
        while (true) {
            try {
                if (!SockUtil.readAll(sock, hbuf)) {
                    throw new IOException("connect closed(read -1)");
                }
            } catch (SocketTimeoutException e) {
                if(System.currentTimeMillis() - lastTime > config.getMaxNoDataTimeout()){
                    log.info("wait message timeout too long,maybe server dead");
                    throw e;
                }
                continue;
            }
            short code = BytesUtils.getShortLittle(hbuf, 0);
            if (code == ProtoConst.ErrCode) {
                short errLen = BytesUtils.getShortLittle(hbuf, 2);
                byte[] errBuf = new byte[errLen];
                SockUtil.readAll(sock, errBuf);
                throw new RuntimeException(new String(errBuf, StandardCharsets.UTF_8));
            }
            if (code == ProtoConst.SubEndCode) {
                log.info("peer notify to end,maybe mq deleted,end sub");
                return;
            }
            lastTime = System.currentTimeMillis();
            if (code == ProtoConst.AliveCode) {
                log.info("sub is alive");
                continue;
            }

            if (code != ProtoConst.OkCode) {
                log.info("not support response code");
                return;
            }
            MsgProcResult result = doMessages(hbuf, processor);
            if (result == MsgProcResult.ClientTermiteWithoutAck) {
                return;
            }

            BytesUtils.putShortLittle(ack, 0, result.getCode());
            SockUtil.writeAll(sock, ack);
            if (result == MsgProcResult.AckWithEnd) {
                return;
            }
        }
    }

    private MsgProcResult doMessages(byte[] hbuf, SubMessageProcessor processor) throws IOException {
        int msgCount = hbuf[2] & 0xFF;
        int payloadSize = BytesUtils.getIntLittle(hbuf, 4);
        byte[] body = new byte[payloadSize];
        SockUtil.readAll(sock, body);
        List<SubMessage> list = parseMessages(body, msgCount);
        return processor.process(list);
    }

    private List<SubMessage> parseMessages(byte[] content, int msgCount) {
        List<SubMessage> list = new ArrayList<>(msgCount);
        int offset = 0;
        for (int i = 0; i < msgCount; i++) {
            SubMessage msg = new SubMessage();
            int n = parseOneMessage(content, offset, msg);
            if (n <= 0) {
                throw new RuntimeException("parse message err");
            }
            list.add(msg);
            offset += n;
        }
        return list;
    }

    public void shutDown(){
        releaseSocket();
    }

    private byte[] pocketSubCmd(String mqName, String who, long eventId) {
        byte[] hbuf = new byte[ProtoConst.REQ_HEADER_SIZE];
        byte[] nameBuf = mqName.getBytes(StandardCharsets.UTF_8);
        byte[] whoBuf = who.getBytes(StandardCharsets.UTF_8);
        hbuf[0] = ProtoConst.CommandSub;

        BytesUtils.putShortLittle(hbuf, 1, nameBuf.length);
        hbuf[3] = (byte) config.getBatchSize();
        hbuf[4] = 1;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        long ackTimeout = config.getAckTimeout() * 1000 * 1000;
        try {
            bos.write(hbuf);
            bos.write(nameBuf);
            bos.write(BytesUtils.long2BytesLittle(eventId));
            bos.write(BytesUtils.long2BytesLittle(ackTimeout));
            bos.write(BytesUtils.int2BytesLittle(whoBuf.length));
            bos.write(whoBuf);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void releaseSocket() {
        if(!state.compareAndSet(RunningState.Running.value,RunningState.Complete.value)){
            return;
        }
        try {
            sock.close();
        } catch (IOException e) {
            log.info("close socket error", e);
        }
    }

    private int parseOneMessage(byte[] body, int offset, SubMessage message) {
        if (body.length - offset < 40) {
            throw new RuntimeException("buff data is not enough");
        }

        int startPos = offset;

        message.setTs(BytesUtils.getLongLittle(body, offset));
        offset += 8;
        message.setEventId(BytesUtils.getLongLittle(body, offset));
        offset += 8;

        message.setFileId(BytesUtils.getLongLittle(body, offset));
        offset += 8;

        message.setFilePos(BytesUtils.getLongLittle(body, offset));
        offset += 8;

        int payloadSize = BytesUtils.getIntLittle(body, offset);
        offset += 4;
        int headerCount = BytesUtils.getIntLittle(body, offset);
        offset += 4;

        int headerSize = 0;

        for (int i = 0; i < headerCount; i++) {
            int l = BytesUtils.getIntLittle(body, offset);
            offset += 4;
            headerSize += 4;
            if (l == 0) {
                continue;
            }
            String name = new String(body, offset, l, StandardCharsets.UTF_8);
            offset += l;
            headerSize += l;
            l = BytesUtils.getIntLittle(body, offset);
            offset += 4;
            headerSize += 4;
            String value = new String(body, offset, l, StandardCharsets.UTF_8);
            offset += l;
            headerSize += l;
            message.addHeader(name, value);
        }

        int dataSize = payloadSize - headerSize;

        byte[] data = new byte[dataSize];
        System.arraycopy(body, offset, data, 0, dataSize);

        message.setPayload(data);

        return offset + dataSize - startPos;
    }
}
