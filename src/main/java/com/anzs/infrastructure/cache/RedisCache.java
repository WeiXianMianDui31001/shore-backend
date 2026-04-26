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

    public java.util.List<String> rangeList(String key, long start, long end) {
        return stringRedisTemplate.opsForList().range(key, start, end);
    }

    public Long listSize(String key) {
        return stringRedisTemplate.opsForList().size(key);
    }

    public void trimList(String key, long start, long end) {
        stringRedisTemplate.opsForList().trim(key, start, end);
    }

    public java.util.Set<String> setMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    public Long setSize(String key) {
        return stringRedisTemplate.opsForSet().size(key);
    }

    public void setHash(String key, String hashKey, String value) {
        stringRedisTemplate.opsForHash().put(key, hashKey, value);
    }

    public String getHash(String key, String hashKey) {
        Object val = stringRedisTemplate.opsForHash().get(key, hashKey);
        return val == null ? null : val.toString();
    }

    public java.util.Map<Object, Object> getHashAll(String key) {
        return stringRedisTemplate.opsForHash().entries(key);
    }

    public Long deleteHash(String key, Object... hashKeys) {
        return stringRedisTemplate.opsForHash().delete(key, hashKeys);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }

    public java.util.Set<String> keys(String pattern) {
        return stringRedisTemplate.keys(pattern);
    }
}
