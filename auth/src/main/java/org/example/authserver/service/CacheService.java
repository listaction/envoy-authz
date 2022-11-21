package org.example.authserver.service;

import authserver.acl.AclRelationConfig;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface CacheService {
  void updateConfigs(Map<String, AclRelationConfig> configMap);

  void updateConfig(AclRelationConfig config);

  void deleteConfig(String namespace);

  Map<String, AclRelationConfig> getConfigs();

  Set<String> getCachedRelations(
      String principal, String namespace, String object, String path, long maxAclUpdate);

  void purgeCacheAsync(String principal, long rev);

  void purgeCache(String principal, long rev);

  void persistCacheAsync(String principal, Collection<String> relations, String path, Long rev);

  void persistCache(String principal, Collection<String> relations, String path, Long revision);
}
