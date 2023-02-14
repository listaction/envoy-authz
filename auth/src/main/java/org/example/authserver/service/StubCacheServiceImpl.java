package org.example.authserver.service;

import authserver.acl.AclRelationConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class StubCacheServiceImpl implements CacheService {
  @Override
  public void updateConfigs(Map<String, AclRelationConfig> configMap) {}

  @Override
  public void updateConfig(AclRelationConfig config) {}

  @Override
  public void deleteConfig(String namespace) {}

  @Override
  public Map<String, AclRelationConfig> getConfigs() {
    return Collections.EMPTY_MAP;
  }

  @Override
  public Set<String> getCachedRelations(
      String principal, String namespace, String object, String path, long maxAclUpdate) {
    return Collections.EMPTY_SET;
  }

  @Override
  public void purgeCacheAsync(String principal, long rev) {}

  @Override
  public void purgeCache(String principal, long rev) {}

  @Override
  public void persistCacheAsync(
      String principal, Collection<String> relations, String path, Long rev) {}

  @Override
  public void persistCacheAsync(
      String principal,
      Collection<String> relations,
      Collection<String> fineGrainedRelations,
      String path,
      Long rev) {}

  @Override
  public void persistCache(
      String principal, Collection<String> relations, String path, Long revision) {}

  @Override
  public void persistCache(
      String principal,
      Collection<String> relations,
      Collection<String> fineGrainedRelations,
      String path,
      Long revision) {}
}
