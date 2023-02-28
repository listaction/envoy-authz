package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.Constants;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.MappingRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class MappingCacheLoader {

  private static final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor();
  private static final ScheduledExecutorService checkCacheRefreshExecutor =
      Executors.newSingleThreadScheduledExecutor();

  private final AtomicBoolean refreshCacheRunning;
  private final AtomicLong refreshCacheRunningTime;
  private final AtomicLong refreshCacheLastTime;

  private final Map<String, MappingEntity> cache;
  private final MappingRepository mappingRepository;
  private final RedisService redisService;

  public MappingCacheLoader(
      MappingRepository mappingRepository,
      Map<String, MappingEntity> cache,
      RedisService redisService) {
    this.mappingRepository = mappingRepository;
    this.cache = cache;
    this.redisService = redisService;

    refreshCacheRunning = new AtomicBoolean(false);
    refreshCacheRunningTime = new AtomicLong(0);
    refreshCacheLastTime = new AtomicLong(0);
  }

  public void schedule(int period, TimeUnit timeUnit) {
    executor.scheduleAtFixedRate(this::refreshCache, 0, period, timeUnit);
  }

  public void scheduleCheckCacheRefresh(int period, TimeUnit timeUnit) {
    checkCacheRefreshExecutor.scheduleAtFixedRate(
        this::checkAndRefreshCacheIfNeeded, 0, period, timeUnit);
  }

  public void checkAndRefreshCacheIfNeeded() {
    try {
      Long lastRefreshCacheRequestTime = getNeedRefreshCacheRequestTime();
      long lastRefreshCacheTime = refreshCacheLastTime.get();
      boolean needToRefresh =
          lastRefreshCacheRequestTime != null
              && lastRefreshCacheTime < lastRefreshCacheRequestTime;

      if (needToRefresh) {
        log.info("Refresh cache by request");
        refreshCacheLastTime.set(lastRefreshCacheRequestTime);
        refreshCache();
      }
    } catch (Exception e) {
      log.info(e.getMessage(), e);
    }
  }

  public void refreshCache() {
    if (refreshCacheRunning.get()) {
      log.info("Refreshing mappings cache is running");
      if ((System.currentTimeMillis() - refreshCacheRunningTime.get()) > 2 * 60 * 1000L){
        refreshCacheRunning.set(false);
      }
      return;
    }

    log.info("Refreshing mappings cache");
    refreshCacheRunning.set(true);
    refreshCacheRunningTime.set(System.currentTimeMillis());

    try {
      List<MappingEntity> mappings = mappingRepository.findAll();
      Set<String> ids = mappings.stream().map(MappingEntity::getId).collect(Collectors.toSet());

      cache.keySet().stream()
          .filter(key -> !ids.contains(key))
          .forEach(
              id -> {
                log.info("Removing mapping {} from cache", id);
                cache.remove(id);
              });

      mappings.forEach(mappingEntity -> cache.put(mappingEntity.getId(), mappingEntity));
    } catch (Exception e){
      log.warn("Can't load mappings", e);
    }

    refreshCacheRunning.set(false);
    log.info("Refresh cache - done");
  }


  private Long getNeedRefreshCacheRequestTime() {
    String lastRefreshCacheRequestTime = redisService.get(Constants.NEED_REFRESH_MAPPING_CACHE_MARKER_KEY);

    return lastRefreshCacheRequestTime != null ? Long.valueOf(lastRefreshCacheRequestTime) : null;
  }

}
