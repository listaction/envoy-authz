package org.example.authserver.service.zanzibar;

import lombok.extern.slf4j.Slf4j;

import authserver.acl.AclRelationConfig;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class AclRelationConfigRepository {

    private static final Map<String, AclRelationConfig> data = new ConcurrentHashMap<>();

    public Set<AclRelationConfig> findAll(){
        return new HashSet<>(data.values());
    }

    protected void save(AclRelationConfig config) {
        data.put(config.getNamespace(), config);
    }

}
