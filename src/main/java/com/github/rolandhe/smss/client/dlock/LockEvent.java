package com.github.rolandhe.smss.client.dlock;

/**
 * 锁监控的事件，不同的实现可以会有不同的事件，但 Locked、LossLock是必须的
 */
public enum LockEvent {
    Locked,   // 获取锁
    Leased,   // 锁续约成功，在不需要续约的分布式实现中，可以不使用该事件
    LossLock, // 丢失锁
    LockTimeout, // 尝试锁定超时
    LockerShutdown, // 锁对象被强制释放
    ;
}
