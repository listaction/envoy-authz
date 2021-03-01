package org.example.authserver.repo;

import authserver.acl.AclRelationConfig;

import java.util.Set;

public interface AclRelationConfigRepository {

    Set<AclRelationConfig> findAll();

    AclRelationConfig findOneById(String id);

    void save(AclRelationConfig config);

    void delete(AclRelationConfig config);

}
