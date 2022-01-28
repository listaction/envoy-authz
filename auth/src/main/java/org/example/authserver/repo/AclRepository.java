package org.example.authserver.repo;

import authserver.acl.Acl;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Set;

public interface AclRepository {

    Set<Acl> findAll();

    Acl findOneById(String id);

    Set<Acl> findAllByNamespaceAndObjectAndUser(String namespace, String object, String user);

    void save(Acl acl);

    void delete(Acl acl);

    Set<Acl> findAllByPrincipalAndNsObjectIn(String principal, List<String> nsObjects);
    Set<Acl> findAllByPrincipal(String principal);
    Set<Acl> findAllByNsObjectIn(List<String> nsObjects);

    Set<String> findAllEndUsers();

    Set<String> findAllNamespaces();

    Set<String> findAllObjects();

    Set<Acl> findAllForCache(String usersetNamespace, String usersetObject, String usersetRelation);


    Long findMaxAclUpdatedByPrincipal(String principal);
}
