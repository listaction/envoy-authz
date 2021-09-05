package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.pgsql.MappingRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class MappingCacheLoader {

    private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, MappingEntity> cache;
    private final MappingRepository mappingRepository;

    public MappingCacheLoader(MappingRepository mappingRepository, Map<String, MappingEntity> cache) {
        this.mappingRepository = mappingRepository;
        this.cache = cache;
    }

    public void schedule(int t, TimeUnit timeUnit) {
        executor.scheduleAtFixedRate(() -> {
            log.info("Refreshing mappings cache");
            refreshCache();
        }, 0, t,  timeUnit);
    }

    public List<MappingEntity> refreshCache(){
        List<MappingEntity> mappings = mappingRepository.findAll();
        // cleanup old mappings
        for (String id : cache.keySet()){
            boolean found = false;
            for (MappingEntity entity : mappings){
                if (entity.getId().equals(id)){
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.info("Removing mapping {} from cache", id);
                cache.remove(id);
            }
        }

        for (MappingEntity entity : mappings){
            cache.put(entity.getId(), entity);
        }

        return mappings;
    }
}
