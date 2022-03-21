package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nullable;

@Slf4j
@Service
public class SignoutService {
    private final RedisService redisService;

    private static final String SIGNOUT_REDIS_KEY = "%s__%s";

    public SignoutService(@Nullable JedisPool jedisPool) {
        this.redisService = new RedisService(jedisPool);
    }

    public void signout(String tenant, String jti, long expirationTime) {
        try {
            String key = String.format(SIGNOUT_REDIS_KEY, tenant, jti);
            redisService.set(key, "1", getSignoutKeyTtl(expirationTime));
        } catch (Exception exception) {
            log.warn("Redis service is unavailable", exception);
        }
    }

    private int getSignoutKeyTtl(long expirationTime) {
        long currentTime = System.currentTimeMillis() / 1000; // current time in seconds

        long diff = expirationTime - currentTime;

        return diff > 0 ? Long.valueOf(diff).intValue() : 1;
    }
}
