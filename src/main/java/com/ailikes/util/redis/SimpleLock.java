package com.ailikes.util.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 功能描述: 分布式锁的简单用法
 * 
 * date:   2018年4月11日 下午5:31:54
 * @author: ailikes
 * @version: 1.0.0
 * @since: 1.0.0
 */
public class SimpleLock {
    private static Logger           logger = LoggerFactory.getLogger(SimpleLock.class);

    private static RedisShardClient redisClient;
    private ShardedJedisLock        lock;
    private String                  lockKey;
    private int                     timeoutMsecs;
    private int                     expireMsecs;

    public SimpleLock(String lockKey) {
    	this.lockKey = "LOCK:" + lockKey;
        this.lock = new ShardedJedisLock(redisClient, this.lockKey.intern());
    }

    public SimpleLock(String lockKey, int timeoutMsecs, int expireMsecs) {
        this.lockKey = "LOCK:" + lockKey;
        this.timeoutMsecs = timeoutMsecs;
        this.expireMsecs = expireMsecs;
        this.lock = new ShardedJedisLock(redisClient, this.lockKey.intern(), timeoutMsecs, expireMsecs);
    }

    public void wrap(Runnable runnable) {
        long begin = System.currentTimeMillis();
        try {
            // timeout超时，等待入锁的时间，设置为3秒；expiration过期，锁存在的时间设置为5分钟
            logger.info("begin logck,lockKey={},timeoutMsecs={},expireMsecs={}", lockKey, timeoutMsecs, expireMsecs);
            if (lock.acquire()) { // 启用锁
                runnable.run();
            } else {
                logger.info("The time wait for lock more than [{}] ms ", timeoutMsecs);
            }
        } catch (Throwable t) {
            // 分布式锁异常
            logger.warn(t.getMessage(), t);
        } finally {
            this.lockRelease(lock);
        }
        logger.info("[{}]cost={}", lockKey, System.currentTimeMillis() - begin);
    }

    private void lockRelease(ShardedJedisLock lock) {
        if (lock != null) {
            try {
                lock.release();// 则解锁
            } catch (Exception e) {
            }
        }
        logger.info("release logck,lockKey={},timeoutMsecs={},expireMsecs={}", lockKey, timeoutMsecs, expireMsecs);
    }

    public static synchronized void setShardedJedisClient(RedisShardClient redisClient) {
        SimpleLock.redisClient = redisClient;
    }

    public static synchronized void destory() {
        SimpleLock.redisClient = null;
    }

}
