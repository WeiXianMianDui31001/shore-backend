package com.anzs.infrastructure.cache;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisCache {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, String value, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public <T> void setObject(String key, T obj, long timeout, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(obj), timeout, unit);
    }

    public <T> T getObject(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null) return null;
        return JSONUtil.toBean(json, clazz);
    }

    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    public Long increment(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(key, delta);
    }

    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    public Boolean setnx(String key, String value, long timeout, TimeUnit unit) {
        return stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    public Long addSet(String key, String... members) {
        return stringRedisTemplate.opsForSet().add(key, members);
    }

    public Long removeSet(String key, Object... members) {
        return stringRedisTemplate.opsForSet().remove(key, members);
    }

    public Long pushList(String key, String value) {
        return stringRedisTemplate.opsForList().rightPush(key, value);
    }
}
