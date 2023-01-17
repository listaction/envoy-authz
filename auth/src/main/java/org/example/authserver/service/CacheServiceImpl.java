package org.example.authserver.service;

import authserver.acl.Acl;
import authserver.acl.AclRelationConfig;
import io.micrometer.core.annotation.Timed;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.MurmurHash3;
import org.example.authserver.Utils;
import org.example.authserver.entity.RelCache;
import org.example.authserver.repo.RelCacheRepository;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.util.CollectionUtils;

@Slf4j
@AllArgsConstructor
public class CacheServiceImpl implements CacheService {
  private final MeterService meterService;
  private final RelCacheRepository relCacheRepository;

  private static final Executor executor = Executors.newFixedThreadPool(1);
  private static final Map<String, AclRelationConfig> configs = new ConcurrentHashMap<>();

  @Override
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

  @Override
  public Map<String, AclRelationConfig> getConfigs() {
    return configs;
  }

  @Override
  @Timed(
      value = "relation.getCached",
      percentiles = {0.99, 0.95, 0.75})
  public Set<String> getCachedRelations(
      String principal, String namespace, String object, String path, long maxAclUpdate) {
    List<RelCache> cache =
        relCacheRepository.findAllByUsrAndRevAndNsobjectAndPath(
            principal, maxAclUpdate, Utils.createNsObject(namespace, object), path);

    if (CollectionUtils.isEmpty(cache)) {
      return new HashSet<>();
    }

    meterService.countHitsCache();

    return cache.stream()
        .map(
            relCache -> {
              Set<String> tags = new HashSet<>();
              tags.add(Utils.createTag(relCache.getNsobject(), relCache.getRelation()));
              Optional.ofNullable(relCache.getNestedRelations()).ifPresent(tags::addAll);

              return tags;
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public void purgeCacheAsync(String principal, long rev) {
    executor.execute(() -> purgeCache(principal, rev));
  }

  @Override
  public void purgeCache(String principal, long rev) {
    log.info("purge cache for {}, rev < {}", principal, rev);
    List<RelCache> recordsToDelete = relCacheRepository.findAllByUsrAndRevLessThan(principal, rev);
    relCacheRepository.deleteAll(recordsToDelete);
  }

  @Override
  public void persistCacheAsync(
      String principal, Collection<String> relations, String path, Long rev) {
    executor.execute(() -> persistCache(principal, relations, path, rev));
  }

  @Override
  public void persistCache(
      String principal, Collection<String> relations, String path, Long revision) {
    List<RelCache> cache = new ArrayList<>();
    for (String tag : relations) {
      Set<String> nestedTags = new HashSet<>(relations);
      nestedTags.remove(tag); // remove current
      String compositeIdKey = getCompositeIdKey(principal, tag, path, revision);
      String id = getKeyHash(compositeIdKey);
      Acl parsedTag = Utils.parseTag(tag);
      if (parsedTag == null) {
        continue;
      }

      cache.add(
          RelCache.builder()
              .id(id)
              .rev(revision)
              .nsobject(Utils.createNsObject(parsedTag.getNamespace(), parsedTag.getObject()))
              .relation(parsedTag.getRelation())
              .nestedRelations(nestedTags)
              .usr(principal)
              .path(path)
              .build());
    }

    try {
      relCacheRepository.saveAll(cache);
    } catch (JpaSystemException e) {
      // very rare exceptions linked with race condition between instances.
      // If revision in composite key doesn't help with uniqueness, must be changed to optimistic
      // lock.
      log.warn("Cache saving problems", e);
    }
  }

  @Override
  public void persistFineGrainedCacheAsync(
      String principal, String relation, Collection<String> nested, String path, Long revision) {
    executor.execute(() -> persistFineGrainedCache(principal, relation, nested, path, revision));
  }

  @Override
  public void persistFineGrainedCache(
      String principal, String tag, Collection<String> nested, String path, Long revision) {
    Set<String> nestedTags = new HashSet<>(nested);
    nestedTags.remove(tag); // remove current
    String compositeIdKey = getCompositeIdKey(principal, tag, path, revision);
    String id = getKeyHash(compositeIdKey);
    Acl parsedTag = Utils.parseTag(tag);
    if (parsedTag == null) {
      return;
    }

    RelCache relCache =
        RelCache.builder()
            .id(id)
            .rev(revision)
            .nsobject(Utils.createNsObject(parsedTag.getNamespace(), parsedTag.getObject()))
            .relation(parsedTag.getRelation())
            .nestedRelations(nestedTags)
            .usr(principal)
            .path(path)
            .build();

    try {
      relCacheRepository.save(relCache);
    } catch (JpaSystemException e) {
      // very rare exceptions linked with race condition between instances.
      // If revision in composite key doesn't help with uniqueness, must be changed to optimistic
      // lock.
      log.warn("Cache saving problems", e);
    }
  }

  private String getKeyHash(String compositeIdKey) {
    long[] vec = MurmurHash3.hash128(compositeIdKey.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (long l : vec) {
      sb.append(Long.toHexString(l));
    }
    return sb.toString();
  }

  private String getCompositeIdKey(String principal, String tag, String path, Long revision) {
    return String.format("%s+%s+%s+%s", principal, tag, path, revision);
  }
}
