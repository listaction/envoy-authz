package org.example.authserver.repo.pgsql;

import org.example.authserver.entity.AclEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AclSpringDataRepository extends CrudRepository<AclEntity, String> {

    List<AclEntity> findAll();
    Set<AclEntity> findAllByNsobjectAndUser(String nsobject, String user);
    Set<AclEntity> findAllByNsobjectInAndUser(List<String> nsobject, String user);
    Set<AclEntity> findAllByUser(String principal);

    @Query("SELECT DISTINCT a.user FROM acls a WHERE a.user <> '*'")
    Set<String> findDistinctEndUsers();

    @Query("SELECT DISTINCT a.namespace FROM acls a")
    Set<String> findDistinctNamespaces();

    @Query("SELECT DISTINCT a.object FROM acls a")
    Set<String> findDistinctObjects();

    @Query("SELECT max(a.updated) FROM acls a where a.user = ?1")
    long findMaxAclUpdatedByPrincipal(String principal);
}
