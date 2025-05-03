package org.example.authserver.repo;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.example.authserver.entity.AclEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AclRepository extends CrudRepository<AclEntity, String> {

  Page<AclEntity> findAll(@NotNull Specification<AclEntity> spec, @Nullable Pageable pageable);

  Page<AclEntity> findByNamespaceLikeAndObjectLikeAndRelationIn(
      String namespace, String object, List<String> relations, @Nullable Pageable pageable);

  Page<AclEntity> findByNamespaceLike(String namespace, PageRequest pageRequest);

  Page<AclEntity> findByNamespaceLikeAndObjectLike(
      String namespace, String object, PageRequest pageRequest);

  Page<AclEntity> findByNsobjectLike(String nsobject, PageRequest pageRequest);

  long countByNamespaceLikeAndObjectLikeAndRelationIn(
      String namespace, String object, List<String> relations);

  List<AclEntity> findAll();

  Set<AclEntity> findAllByNsobjectAndUser(String nsobject, String user);

  Set<AclEntity> findAllByNsobjectInAndUser(List<String> nsobject, String user);

  Set<AclEntity> findAllByUser(String principal);

  Set<AclEntity> findAllByUsersetNamespaceAndUsersetObjectAndUsersetRelationAndUser(
      String usersetNamespace, String usersetObject, String usersetRelation, String user);

  @Query("SELECT DISTINCT a.user FROM acls a WHERE a.user <> '*'")
  Set<String> findDistinctEndUsers();

  @Query("SELECT DISTINCT a.namespace FROM acls a")
  Set<String> findDistinctNamespaces();

  @Query("SELECT DISTINCT a.object FROM acls a")
  Set<String> findDistinctObjects();

  @Query("SELECT max(a.updated) FROM acls a where a.user = ?1")
  Long findMaxAclUpdatedByPrincipal(String principal);

  void
      deleteAllByNamespaceAndObjectAndRelationAndUserAndUsersetNamespaceAndUsersetObjectAndUsersetRelation(
          String namespace,
          String object,
          String rel,
          String user,
          String usNamespace,
          String usObject,
          String usRel);
}
