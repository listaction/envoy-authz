package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.Constants;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.MappingRepository;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MappingCacheService {

  private final Map<String, MappingEntity> cache = new ConcurrentHashMap<>();
  private final MappingCacheLoader mappingCacheLoader;

  private final RedisService redisService;

  public MappingCacheService(@Nullable JedisPool jedisPool, MappingRepository mappingRepository) {
    this.redisService = new RedisService(jedisPool);
    this.mappingCacheLoader = new MappingCacheLoader(mappingRepository, cache, this.redisService);
    this.mappingCacheLoader.schedule(10, TimeUnit.MINUTES);
    this.mappingCacheLoader.scheduleCheckCacheRefresh(5, TimeUnit.SECONDS);
  }

  public List<MappingEntity> getAll() {
    log.trace("getting mappings from cache: {}", cache.size());
    if (cache.isEmpty()) {
      log.info("refreshing cache");
      mappingCacheLoader.refreshCache();
    }

    return new ArrayList<>(cache.values());
  }

  public void notifyAllToRefreshCache() {
    log.info("Notify all to refresh cache request");

    redisService.set(
        Constants.NEED_REFRESH_MAPPING_CACHE_MARKER_KEY, String.valueOf(System.currentTimeMillis()), 60);
  }
}
