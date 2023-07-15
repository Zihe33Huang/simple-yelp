package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author huangzihe
 * @date 2023/7/15 2:48 AM
 */
@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP = 1640995200L;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    private static final int COUNT_BITS = 32;

    /**
     *
     * @param keyPrefix:  different table using different keyPrefix
     * @return
     */
    public long nextId(String keyPrefix) {
        // 1. generate timestamp
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. Get today
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

        // 3. auto-increment
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        return timestamp << COUNT_BITS | count;
    }

}
