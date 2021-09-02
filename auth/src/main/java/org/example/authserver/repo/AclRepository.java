package org.example.authserver.repo;

import authserver.acl.Acl;
import org.example.authserver.entity.AclEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface AclRepository {

    Set<Acl> findAll();

    Acl findOneById(String id);

    Set<Acl> findAllByNamespaceAndObjectAndUser(String namespace, String object, String user);

    void save(Acl acl);

    void delete(Acl acl);

    Set<Acl> findAllByPrincipalAndNsObjectIn(String principal, List<String> nsObjects);
    Set<Acl> findAllByPrincipal(String principal);
    Set<Acl> findAllByNsObjectIn(List<String> nsObjects);

}
