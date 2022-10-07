package org.example.authserver.service;


import authserver.acl.Acl;
import authserver.acl.AclRelationConfig;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.MurmurHash3;
import org.example.authserver.Utils;
import org.example.authserver.entity.RelCache;
import org.example.authserver.repo.RelCacheRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CacheService {

    private static final Executor executor = Executors.newFixedThreadPool(1);
    private static final Map<String, AclRelationConfig> configs = new ConcurrentHashMap<>();

    private final MeterService meterService;
    private final RelCacheRepository relCacheRepository;

    public CacheService(MeterService meterService, RelCacheRepository relCacheRepository) {
        this.meterService = meterService;
        this.relCacheRepository = relCacheRepository;
    }

    public void updateConfigs(Map<String, AclRelationConfig> configMap) {
        configs.keySet().removeIf(cfg -> !configMap.containsKey(cfg));
        configs.putAll(configMap);
    }

    public void updateConfig(AclRelationConfig config) {
        configs.put(config.getNamespace(), config);
    }

    public void deleteConfig(String namespace) {
        configs.remove(namespace);
    }

    public Map<String, AclRelationConfig> getConfigs() {
        return configs;
    }


    @Timed(value = "relation.getCached", percentiles = {0.99, 0.95, 0.75})
    public Set<String> getCachedRelations(String principal, String namespace, String object, String path, long maxAclUpdate) {
        List<RelCache> cache = relCacheRepository.findAllByUsrAndRevAndNsobjectAndPath(principal, maxAclUpdate, Utils.createNsObject(namespace, object), path);
        if (cache == null || cache.size() == 0) return new HashSet<>();
        meterService.countHitsCache();
        return cache.stream()
                .map(m->{
                    Set<String> tags = new HashSet<>();
                    tags.add(Utils.createTag(m.getNsobject(), m.getRelation()));

                    if (m.getNestedRelations() != null) {
                        tags.addAll(m.getNestedRelations());
                    }

                    return tags;
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    public void purgeCacheAsync(String principal, long rev){
        executor.execute(() -> {
            purgeCache(principal, rev);
        });
    }

    public void purgeCache(String principal, long rev){
        log.info("purge cache for {}, rev < {}", principal, rev);
        List<RelCache> recordsToDelete = relCacheRepository.findAllByUsrAndRevLessThan(principal, rev);
        relCacheRepository.deleteAll(recordsToDelete);
    }

    public void persistCacheAsync(String principal, Collection<String> relations, String path, Long rev){
        executor.execute(() -> {
            persistCache(principal, relations, path, rev);
        });
    }

    public void persistCache(String principal, Collection<String> relations, String path, Long rev){
        List<RelCache> cache = new ArrayList<>();
        for (String tag : relations){
            Set<String> nestedTags = new HashSet<>(relations);
            nestedTags.remove(tag); // remove current
            String compositeIdKey = getCompositeIdKey(principal, tag, path);
            String id = getKeyHash(compositeIdKey);
            Acl parsedTag = Utils.parseTag(tag);
            if (parsedTag == null) continue;

            RelCache relCache = RelCache.builder()
                    .id(id)
                    .rev(rev)
                    .nsobject(Utils.createNsObject(parsedTag.getNamespace(), parsedTag.getObject()))
                    .relation(parsedTag.getRelation())
                    .nestedRelations(nestedTags)
                    .usr(principal)
                    .path(path)
                    .build();

            cache.add(relCache);
        }

        relCacheRepository.saveAll(cache);
    }

    private String getKeyHash(String compositeIdKey) {
        long[] vec = MurmurHash3.hash128(compositeIdKey.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (long l : vec) {
            sb.append(Long.toHexString(l));
        }
        return sb.toString();
    }

    protected String getCompositeIdKey(String principal, String tag, String path){
        return String.format("%s+%s+%s", principal, tag, path);
    }
}
