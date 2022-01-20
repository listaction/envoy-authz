package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.pgsql.MappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public MappingCacheService(@Nullable JedisPool jedis, MappingRepository mappingRepository) {
        this.mappingCacheLoader = new MappingCacheLoader(mappingRepository, cache, jedis);
        this.mappingCacheLoader.schedule(10, TimeUnit.MINUTES);
    }

    public List<MappingEntity> getAll() {
        log.trace("getting mappings from cache: {}", cache.size());
        if (cache.isEmpty()) {
            log.info("refreshing cache");
            mappingCacheLoader.refreshCache();
        }

        return new ArrayList<>(cache.values());
    }

    public void refreshCache() {
        log.trace("refresh mappings to cache: {}", cache.size());
        mappingCacheLoader.refreshCache();
    }

}
