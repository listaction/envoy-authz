package org.example.authserver.service;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
@Service
public class RedisService {
  private final JedisPool jedisPool;

  public RedisService(@Nullable JedisPool jedisPool) {
    this.jedisPool = jedisPool;
  }

  public String get(String key) {
    if (jedisPool == null) return null;
    try (Jedis jedis = jedisPool.getResource()){
      return jedis.get(key);
    } catch (Exception e){
      log.warn("Can't get value by key {}", key, e);
    }
    return null;
  }

  public void set(String key, String value) {
    if (jedisPool == null) return;
    try (Jedis jedis = jedisPool.getResource()){
      jedis.set(key, value);
    } catch (Exception e){
      log.warn("Can't set {} => {}", key, value, e);
    }
  }

  public void set(String key, String value, int ttlSeconds) {
    if (jedisPool == null) return;
    try (Jedis jedis = jedisPool.getResource()){
      jedis.set(key, value);
      jedis.expire(key, ttlSeconds);
    } catch (Exception e){
      log.warn("Can't set {} => {}", key, value, e);
    }
  }

  public void del(String key) {
    if (jedisPool == null) return;
    try (Jedis jedis = jedisPool.getResource()){
      jedis.del(key);
    } catch (Exception e){
      log.warn("Can't delete {}", key, e);
    }
  }

  public boolean exists(String key) {
    if (jedisPool == null) return false;
    try (Jedis jedis = jedisPool.getResource()){
      return jedis.exists(key);
    } catch (Exception e){
      log.warn("Can't check exists {}", key, e);
    }

    return false;
  }
}
