package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.pgsql.MappingRepository;
import org.springframework.stereotype.Service;

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

    public MappingCacheService(MappingRepository mappingRepository) {
        this.mappingCacheLoader = new MappingCacheLoader(mappingRepository, cache);
        this.mappingCacheLoader.schedule(10, TimeUnit.MINUTES);
    }

    public List<MappingEntity> getAll() {
        log.trace("getting mappings from cache: {}", cache.size());
        if (cache.size() == 0){
            log.info("refreshing cache");
            return mappingCacheLoader.refreshCache();
        }
        return new ArrayList<>(cache.values());
    }

}
