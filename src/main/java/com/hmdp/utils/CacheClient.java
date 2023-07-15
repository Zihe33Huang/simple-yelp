package com.hmdp.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author huangzihe
 * @date 2023/7/15 12:23 AM
 */
@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    /**
     *  This function prevents Cache Penetration
     */
    public <R, ID> R query(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit timeUnit) {
        String key = keyPrefix + id;

        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        // check if the value is a blank value.
        if (json.equals("") ) {
            return null;
        }

        // The data not cached in Redis
        R r = dbFallback.apply(id);
        if (r == null) {
            // set blank value into cache to prevent Cache Penetration
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        this.set(key, r, time, timeUnit);
        return r;
    }

    /**
     * This function can not only prevent Cache penetration, but also Hotspot Invalid
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit timeUnit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        if (json.equals("")) {
            return null;
        }
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        R r = null;
        String token = RandomUtil.randomString(6);
        try {
            boolean flag = tryLock(lockKey, token);
            // fail to acquire lock, sleep
            if (!flag) {
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, timeUnit);
            }
            // get lock
            r = dbFallback.apply(id);
            if (r == null) {
                // set blank value into cache to prevent Cache Penetration
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            this.set(key, r, time, timeUnit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey, token);
        }
        return r;
    }

    private boolean tryLock(String key, String value) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, value, 10, TimeUnit.SECONDS);
        return flag;
    }

    private void unlock(String key, String value) {
        String v = stringRedisTemplate.opsForValue().get(key);
        if (value.equals(v)) {
            stringRedisTemplate.delete(key);
            return;
        }
    }


}
