package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.pgsql.MappingRepository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MappingCacheLoader {

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, MappingEntity> cache;
    private final MappingRepository mappingRepository;
    private final Jedis jedis;

    private static final String REFRESH_PROCESSING_MARKER_KEY = "REFRESH_PROCESSING_MARKER";

    private static final String REFRESH_PROCESSING_MARKER_ON = "true";

    private static final String REFRESH_PROCESSING_MARKER_OFF = "false";

    public MappingCacheLoader(MappingRepository mappingRepository, Map<String, MappingEntity> cache, JedisPool jedis) {
        this.mappingRepository = mappingRepository;
        this.cache = cache;
        this.jedis = jedis.getResource();
    }

    public void schedule(int period, TimeUnit timeUnit) {
        executor.scheduleAtFixedRate(this::refreshCache, 0, period, timeUnit);
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

    private boolean isRefreshCacheRunning() {
        return REFRESH_PROCESSING_MARKER_ON.equals(jedis.get(REFRESH_PROCESSING_MARKER_KEY));
    }

    private void markRefreshCacheRunning() {
        jedis.set(REFRESH_PROCESSING_MARKER_KEY, REFRESH_PROCESSING_MARKER_ON);
    }

    private void unmarkRefreshCacheRunning() {
        jedis.set(REFRESH_PROCESSING_MARKER_KEY, REFRESH_PROCESSING_MARKER_OFF);
    }
}
