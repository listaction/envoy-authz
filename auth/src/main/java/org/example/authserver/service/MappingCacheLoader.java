package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.MappingRepository;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.example.authserver.service.RedisService.*;

@Slf4j
public class MappingCacheLoader {

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService checkCacheRefreshExecutor = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService deleteCacheRefreshExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, MappingEntity> cache;
    private final MappingRepository mappingRepository;
    private static final Integer REFRESH_CACHE_MARKER_TTL = 60 * 1000; // 60 seconds
    private static final String REDIS_IDENTIFIER = UUID.randomUUID().toString();
    private final RedisService redisService;

    public MappingCacheLoader(MappingRepository mappingRepository, Map<String, MappingEntity> cache, RedisService redisService) {
        this.mappingRepository = mappingRepository;
        this.cache = cache;
        this.redisService = redisService;
    }

    public void schedule(int period, TimeUnit timeUnit) {
        executor.scheduleAtFixedRate(this::refreshCache, 0, period, timeUnit);
    }

    public void scheduleCheckCacheRefresh(int period, TimeUnit timeUnit) {
        checkCacheRefreshExecutor.scheduleAtFixedRate(this::checkAndRefreshCacheIfNeeded, 0, period, timeUnit);
    }

    public void scheduleDeleteCacheRefresh(int period, TimeUnit timeUnit) {
        deleteCacheRefreshExecutor.scheduleAtFixedRate(this::checkAndDeleteNeedRefreshCacheMarker, 0, period, timeUnit);
    }

    public void checkAndRefreshCacheIfNeeded() {
        try {
            Long lastRefreshCacheRequestTime = getNeedRefreshCacheRequestTime();
            Long lastRefreshCacheTime = getLastRefreshCacheTime();
            boolean needToRefresh = lastRefreshCacheRequestTime != null &&
                    !Objects.equals(lastRefreshCacheRequestTime, lastRefreshCacheTime);

//            log.info("checkAndRefreshCacheIfNeeded :: {}", needToRefresh);

            if (needToRefresh) {
                log.info("Refresh cache by request");
                redisService.set(
                        getRedisInstanceKey(TIME_OF_REFRESH_MAPPING_CACHE_BY_REQUEST_KEY),
                        lastRefreshCacheRequestTime.toString()
                );

                refreshCache();
            }
        } catch (Exception ex) {
            log.info(ex.getMessage(), ex);
        }
    }

    public void refreshCache() {
        if (isRefreshCacheRunning()) {
            log.info("Refreshing mappings cache is running");

            return;
        }

        try {
            log.info("Refreshing mappings cache");
            markRefreshCacheRunning();

            List<MappingEntity> mappings = mappingRepository.findAll();
            Set<String> ids = mappings.stream().map(MappingEntity::getId).collect(Collectors.toSet());

            cache.keySet().stream()
                    .filter(key -> !ids.contains(key))
                    .forEach(id -> {
                        log.info("Removing mapping {} from cache", id);
                        cache.remove(id);
                    });

            mappings.forEach(mappingEntity -> cache.put(mappingEntity.getId(), mappingEntity));
        } finally {
            unmarkRefreshCacheRunning();
        }
    }

    private void checkAndDeleteNeedRefreshCacheMarker() {
        try {
            Long lastRefreshCacheRequestTime = getNeedRefreshCacheRequestTime();
            boolean needToDeleteMarker = lastRefreshCacheRequestTime != null &&
                    (System.currentTimeMillis() - lastRefreshCacheRequestTime) > REFRESH_CACHE_MARKER_TTL;
//            log.info("checkAndDeleteNeedRefreshCacheMarker :: {}", needToDeleteMarker);

            if (needToDeleteMarker) {
                log.info("Auto remove need to refresh cache marker");
                redisService.del(NEED_REFRESH_MAPPING_CACHE_MARKER_KEY);
            }
        } catch (Exception ex) {
            log.info(ex.getMessage(), ex);
        }
    }

    private boolean isRefreshCacheRunning() {
        String marker = redisService.get(REFRESH_PROCESSING_MARKER_KEY);

        return marker != null && MARKER_ON.equals(marker);
    }

    private Long getNeedRefreshCacheRequestTime() {
        String lastRefreshCacheRequestTime = redisService.get(NEED_REFRESH_MAPPING_CACHE_MARKER_KEY);

        return lastRefreshCacheRequestTime != null ? Long.valueOf(lastRefreshCacheRequestTime) : null;
    }

    private Long getLastRefreshCacheTime() {
        String lastRefreshCacheTime = redisService.get(getRedisInstanceKey(TIME_OF_REFRESH_MAPPING_CACHE_BY_REQUEST_KEY));

        return lastRefreshCacheTime != null ? Long.valueOf(lastRefreshCacheTime) : null;
    }

    private void markRefreshCacheRunning() {
        redisService.set(getRedisInstanceKey(REFRESH_PROCESSING_MARKER_KEY), MARKER_ON);
    }

    private void unmarkRefreshCacheRunning() {
        redisService.del(getRedisInstanceKey(REFRESH_PROCESSING_MARKER_KEY));
    }

    private String getRedisInstanceKey(String key) {
        return String.format("%s_%s", key, REDIS_IDENTIFIER);
    }

}
