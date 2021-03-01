package org.example.authserver.repo;

import authserver.acl.Acl;

import java.util.Set;

public interface AclRepository {

    Set<Acl> findAll();

    Acl findOneById(String id);

    Set<Acl> findAllByNamespaceAndObjectAndUser(String namespace, String object, String user);

    void save(Acl acl);

    void delete(Acl acl);

}
