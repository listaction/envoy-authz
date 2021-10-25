package org.example.authserver.service;


import authserver.acl.Acl;
import authserver.acl.AclRelationConfig;
import lombok.extern.slf4j.Slf4j;

import org.example.authserver.repo.AclRepository;
import org.example.authserver.service.model.RequestCache;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CacheService {

    private static final Map<String, AclRelationConfig> configs = new ConcurrentHashMap<>();

    private final AclRepository aclRepository;

    public CacheService(AclRepository aclRepository) {
        this.aclRepository = aclRepository;
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

    public RequestCache prepareHighCardinalityCache(String user) {
        return prepareHighCardinalityCache(new RequestCache(), user);
    }

    public RequestCache prepareHighCardinalityCache(RequestCache requestCache, String user) {
        if (requestCache.getPrincipalHighCardinalityCache().containsKey(user) || "*".equals(user)) {
            return requestCache;
        }

        Set<Acl> acls = aclRepository.findAllByPrincipal(user);
        requestCache.getPrincipalAclCache().put(user, new HashSet<>(acls));
        requestCache.getPrincipalHighCardinalityCache().put(user, Acl.getTags(acls));
        return requestCache;
    }
}
