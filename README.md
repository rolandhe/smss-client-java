# smss java client

java版本的smss客户端，提供以下能力：

* 发送消息及其他操作
    * 发送消息
    * 发送延迟消息，在发送消息时指的延迟的时间，mq收到延迟消息后存储，待到期时自行发送消息
    * 创建topic，topic支持生命周期，创建是可以指定存活时长，到期后自动删除
    * 删除topic
    * 获取指定topic的信息
    * 获取所有topic的信息
    * 探活，判断当前连接或者mq服务端是否还存活
* 订阅消息


# 发送消息及其他操作

PubClient描述了所有的操作支持，构建PubClient对象有两种方法，

* 创建PubClientSocket对象，产出简单的、底层socket是非池化的对象
* 创建PubClientPool构建一个PubClient的池，调用pool的Borrow方法获取池化的对象

生成环境中建议使用PubClientPool，简单的测试可以使用PubClientSocket。

## 示例

## 构建池化PubClient对象

```
    PubClientPool pool = new PubClientPool("localhost", 12301);
    PubClient pclient = null;
    try {
        pclient = pool.borrow();
        // 业务代码
    } catch (RuntimeException e) {
        e.printStackTrace();
    } finally {
        if (pclient != null) {
            pclient.close();
        }
        pool.shutDown();
    }

```

## 发送消息
smss消息支持header，可以通过addHeader方法来添加header

```
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
```

### 发送延迟消息

```
    Message msg = new Message();
    msg.addHeader("h001", "v001");
    String body = String.format("delay 32 hello world-000%d", i);
    msg.setPayload(body.getBytes(StandardCharsets.UTF_8));
    String traceId = String.format("test-publish-00%d", i);
    OpResult ret = pclient.publishDelay("order", msg, 30 * 1000L,traceId);
```

## 创建永久topic

```
    OpResult ret = pclient.createTopic("trade2",0, UUID.randomUUID().toString());
```

### 创建有生命周期的topic

```
    // 10分钟的生命周期
    long expireAt = System.currentTimeMillis() + 60L * 1000 * 10;
    OpResult ret = pclient.createTopic("trade2",expireAt, UUID.randomUUID().toString());
```

## 删除topic

```
    OpResult ret = pclient.deleteTopic("trade2",UUID.randomUUID().toString());
```

## 获取topic信息,返回json

```
    TopicInfoResult ret = pclient.getTopicInfo("trade",UUID.randomUUID().toString());
```

## 获取所有topic信息，返回json

```
    TopicInfoResult ret = pclient.listTopicInfo(UUID.randomUUID().toString());
```

# 订阅

smss的订阅支持类似kafka的group订阅，每一个group只能有一个客户端实例来定义，在smss订阅时，可以指定who参数，代表谁来订阅，
smss 服务端内部会检测是否有两个相同的who来订阅同一个topic，如果有，第二个将被拒绝，但这是一种兜底方案，正常的处理方式是需要
smss客户端使用分布式锁来协调，保证只有一个who可以访问一个topic。

smss客户端支持两种订阅方式：
* 简单订阅，当业务上确认只有一个who订阅消息时可以使用这种模式，一般情况下，指的是只有可以客户端的实例被部署
* 支持分布式锁的订阅，客户端可以部署多个实例，通过分布式锁来协调只有一个实例可以订阅到消息

## 分布式锁订阅
### 分布式锁
SubLock接口定义了分布式锁的描述，你可以自行实现，比如基于zookeeper、etcd或者redis实现自己的分布式锁。

#### redis分布式锁

smss-client-java实现了基于redis的分布式锁，dlock/redis下是对应的实现代码。redis的分布式锁高效、简单，因此被缺省实现，但redis锁也有缺点，
就是不能像zookeeper一样实时的监控锁的状态，redis锁要通过轮询的方式来监听锁的状态。

* 使用setnx+超时尝试获取锁，超时即获取锁的租约时间，key是who，value是一个uuid，代表当前实例id
    * 如果没有获取到锁，则以一个固定周期继续尝试获取到锁
* 获取到锁，则通知业务开启业务处理，同时指定一个固定的时间周期性的续约，这个周期要小于租约时间
    * 为了保证需要的准确性，在续约时需要携带uuid，需要判断当前value==uuid时才能需要，为了保证原子性，可以使用lua脚本
    * 如果使用了不支持lua的类redis，比如pika， 可以降低准确性，通过更短的续约周期来保证当前所是被当前实例持有的
* 如果续约失败，意味着锁丢失，那通知业务终止业务处理，并尝试再次获取锁
* 循环

### 示例

```
    SubLock lock = RedisSubLock.factory("localhost",6379,true);

    Subscribe lockedSubClient = new LockedSubClient(SubConfig.newDefault("localhost",12301),lock,"order", "vvi", 0);

    lockedSubClient.subscribe( new SubMessageProcessor() {
        @Override
        public MsgProcResult process(List<SubMessage> messageList) {
            for(SubMessage msg : messageList){
                StringBuilder sb = new StringBuilder();
                if(!msg.getHeaderMap().isEmpty()){
                    for(Entry<String, Header> h : msg.getHeaderMap().entrySet()){
                        sb.append(h.getValue().toString()).append(",");
                    }
                    sb.delete(sb.length() - 1,sb.length());
                }
                log.info("msg, eventId={},header={},payload={}",msg.getEventId(),sb.toString(),new String(msg.getPayload(), StandardCharsets.UTF_8));
            }
            return MsgProcResult.ACK;
        }
    });

    try {
        Thread.sleep(60 * 1000L);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }finally {
        lock.shutdown();
    }
```

## 简单订阅

```
    SubClient subClient = new SubClient(SubConfig.newDefault("localhost",12301),"order", "java-client", 0);
    subClient.subscribe( new SubMessageProcessor() {
        @Override
        public MsgProcResult process(List<SubMessage> messageList) {
            for(SubMessage msg : messageList){
                StringBuilder sb = new StringBuilder();
                if(!msg.getHeaderMap().isEmpty()){
                    for(Entry<String, Header> h : msg.getHeaderMap().entrySet()){
                        sb.append(h.getValue().toString()).append(",");
                    }
                    sb.delete(sb.length() - 1,sb.length());
                }
                log.info("msg, eventId={},header={},payload={}",msg.getEventId(),sb.toString(),new String(msg.getPayload(), StandardCharsets.UTF_8));
            }
            return MsgProcResult.ACK;
        }
    });
```