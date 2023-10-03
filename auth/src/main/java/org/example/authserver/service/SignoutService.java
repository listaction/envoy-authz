package org.example.authserver.service;

import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.Constants;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

@Slf4j
@Service
public class SignoutService {

  private final RedisService redisService;

  public SignoutService(@Nullable JedisPool jedisPool) {
    this.redisService = new RedisService(jedisPool);
  }

  public void signout(String tenant, String jti, long expirationTime) {
    try {
      String key = String.format(Constants.SIGNOUT_REDIS_KEY, jti);
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

  public void userSignout(String tenant, String userId) {
    try {
      String key = String.format(Constants.USER_SIGNOUT_REDIS_KEY, userId);
      redisService.set(key, "1");
    } catch (Exception exception) {
      log.warn("Redis service is unavailable", exception);
    }
  }
}
