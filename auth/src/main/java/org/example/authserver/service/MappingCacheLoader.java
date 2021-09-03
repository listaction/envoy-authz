package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.pgsql.MappingRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MappingCacheLoader {

    private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final CopyOnWriteArrayList<MappingEntity> cache;
    private final MappingRepository mappingRepository;

    public MappingCacheLoader(MappingRepository mappingRepository, CopyOnWriteArrayList<MappingEntity> cache) {
        this.mappingRepository = mappingRepository;
        this.cache = cache;
    }

    public void schedule(int t, TimeUnit timeUnit) {
        executor.scheduleAtFixedRate(() -> {
            log.info("Refreshing mappings cache");
            List<MappingEntity> mappings = mappingRepository.findAll();
            cache.addAll(mappings);
            cache.removeIf(entry -> !mappings.contains(entry));
        }, 0, t,  timeUnit);
    }
}
