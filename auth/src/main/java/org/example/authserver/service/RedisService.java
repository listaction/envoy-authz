package org.example.authserver.service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nullable;

public class RedisService {

    public static final String REFRESH_PROCESSING_MARKER_KEY = "REFRESH_PROCESSING_MARKER";

    public static final String NEED_REFRESH_MAPPING_CACHE_MARKER_KEY = "NEED_REFRESH_MAPPING_CACHE_MARKER";

    public static final String TIME_OF_REFRESH_MAPPING_CACHE_BY_REQUEST_KEY = "TIME_OF_REFRESH_MAPPING_CACHE_BY_REQUEST_KEY";

    public static final String MARKER_ON = "true";

    public static final String MARKER_OFF = "false";

    private final JedisPool jedisPool;

    public RedisService(@Nullable JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public String get(String key) {
        if (jedisPool == null) return null;
        Jedis jedis = jedisPool.getResource();
        String value = jedis.get(key);
        jedis.close();

        return value;
    }

    public void set(String key, String value) {
        if (jedisPool == null) return;
        Jedis jedis = jedisPool.getResource();
        jedis.set(key, value);
        jedis.close();
    }

    public void set(String key, String value, int ttl) {
        if (jedisPool == null) return;
        Jedis jedis = jedisPool.getResource();
        jedis.set(key, value);
        jedis.expire(key, ttl);
        jedis.close();
    }

    public void del(String key) {
        if (jedisPool == null) return;
        Jedis jedis = jedisPool.getResource();
        jedis.del(key);
        jedis.close();
    }

    public boolean exists(String key) {
        if (jedisPool == null) return false;
        Jedis jedis = jedisPool.getResource();
        boolean exists = jedis.exists(key);
        jedis.close();

        return exists;
    }
}
