package com.github.rolandhe.smss.client;

import com.github.rolandhe.smss.client.msg.Message;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


public class PubClientTestCase {
    @Test
    public void testPub() {
        PubClientPool pool = new PubClientPool("localhost", 12301);
        PubClient pclient = null;
        try {
            pclient = pool.borrow();
            for (int i = 24560; i < 24660; i++) {
                Message msg = new Message();
                String body = String.format("hello world-000%d", i);
                msg.setPayload(body.getBytes(StandardCharsets.UTF_8));
                String traceId = String.format("test-publish-00%d", i);
                OpResult ret = pclient.publish("order", msg, traceId);
                System.out.println(ret);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (pclient != null) {
                pclient.close();
            }
            pool.shutDown();
        }
    }

    @Test
    public void testPubHeader() {
        PubClientPool pool = new PubClientPool("localhost", 12301);
        PubClient pclient = null;
        try {
            pclient = pool.borrow();

            int i = 101;
            Message msg = new Message();
            msg.addHeader("h001", "v001");
            String body = String.format("hello world-000%d", i);
            msg.setPayload(body.getBytes(StandardCharsets.UTF_8));
            String traceId = String.format("test-publish-00%d", i);
            OpResult ret = pclient.publish("order", msg, traceId);
            System.out.println(ret);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (pclient != null) {
                pclient.close();
            }
            pool.shutDown();
        }
    }

    @Test
    public void testPubHeaderDelay() {
        PubClientPool pool = new PubClientPool("localhost", 12301);
        PubClient pclient = null;
        try {
            pclient = pool.borrow();

            int i = 101;
            Message msg = new Message();
            msg.addHeader("h001", "v001");
            String body = String.format("delay 32 hello world-000%d", i);
            msg.setPayload(body.getBytes(StandardCharsets.UTF_8));
            String traceId = String.format("test-publish-00%d", i);
            OpResult ret = pclient.publishDelay("order", msg, 30 * 1000L,traceId);
            System.out.println(ret);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (pclient != null) {
                pclient.close();
            }
            pool.shutDown();
        }
    }

    @Test
    public void testCreateTopic(){
        PubClientPool pool = new PubClientPool("localhost", 12301);
        PubClient pclient = null;
        try {
            pclient = pool.borrow();
            OpResult ret = pclient.createTopic("trade2",0, UUID.randomUUID().toString());
            System.out.println(ret);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (pclient != null) {
                pclient.close();
            }
            pool.shutDown();
        }
    }

    @Test
    public void testDeleteTopic(){
        PubClientPool pool = new PubClientPool("localhost", 12301);
        PubClient pclient = null;
        try {
            pclient = pool.borrow();
            OpResult ret = pclient.deleteTopic("trade2",UUID.randomUUID().toString());
            System.out.println(ret);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (pclient != null) {
                pclient.close();
            }
            pool.shutDown();
        }
    }

    @Test
    public void testGetTopic(){
        PubClientPool pool = new PubClientPool("localhost", 12301);
        PubClient pclient = null;
        try {
            pclient = pool.borrow();
            TopicInfoResult ret = pclient.getTopicInfo("trade",UUID.randomUUID().toString());
            System.out.println(ret);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (pclient != null) {
                pclient.close();
            }
            pool.shutDown();
        }
    }

    @Test
    public void testListTopic(){
        PubClientPool pool = new PubClientPool("localhost", 12301);
        PubClient pclient = null;
        try {
            pclient = pool.borrow();
            TopicInfoResult ret = pclient.listTopicInfo(UUID.randomUUID().toString());
            System.out.println(ret);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            if (pclient != null) {
                pclient.close();
            }
            pool.shutDown();
        }
    }
}
