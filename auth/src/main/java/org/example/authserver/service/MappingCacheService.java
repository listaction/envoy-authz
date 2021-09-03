package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.pgsql.MappingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MappingCacheService {

    private final CopyOnWriteArrayList<MappingEntity> cache = new CopyOnWriteArrayList<>();

    private final MappingRepository mappingRepository;
    private final MappingCacheLoader mappingCacheLoader;

    public MappingCacheService(MappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
        this.mappingCacheLoader = new MappingCacheLoader(mappingRepository, cache);
        this.mappingCacheLoader.schedule(10, TimeUnit.MINUTES);
    }

    public List<MappingEntity> getAll() {
        log.trace("getting mappings from cache: {}", cache.size());
        return cache;
    }
}
