package org.example.authserver.service;

import authserver.acl.Acl;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.AclEntity;
import org.example.authserver.entity.PageView;
import org.example.authserver.exception.NotSortableFieldException;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.filter.AclFilter;
import org.example.authserver.repo.filter.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AclService {

  private final AclRepository repository;

  public AclService(AclRepository repository) {
    this.repository = repository;
  }

  public Set<Acl> findAll() {
    return repository.findAll().stream().map(AclEntity::toAcl).collect(Collectors.toSet());
  }

  public PageView<Acl> findAll(
      String namespace,
      String object,
      List<String> relations,
      String user,
      String usersetNamespace,
      String usersetObject,
      List<String> usersetRelations,
      String sort,
      Sort.Direction direction,
      Integer page,
      Integer pageSize) {
    PageRequest pageRequest;
    if (sort != null) {
      String sortField = AclFilter.getSortableField(sort);
      if (sortField == null) {
        throw new NotSortableFieldException();
      }
      pageRequest =
          PageRequest.of(
              page - 1,
              pageSize,
              Sort.by(direction != null ? direction : Sort.Direction.ASC, sortField));
    } else {
      pageRequest = PageRequest.of(page - 1, pageSize);
    }

    Map<String, String> filterParams = new HashMap<>();
    Filter<AclEntity> filter = new Filter<>();
    if (namespace != null && !namespace.isEmpty() && object != null && !object.isEmpty()) {
      filterParams.put("nsobject", namespace + ":" + object);
    } else {
      if (namespace != null && !namespace.isEmpty()) {
        filterParams.put("namespace", namespace);
      }
      if (object != null && !object.isEmpty()) {
        filterParams.put("object", object);
      }
    }
    if (relations != null && !relations.isEmpty()) {
      filterParams.put("relation", String.join(",", relations));
    }
    if (user != null && !user.isEmpty()) {
      filterParams.put("user", String.join(",", user));
    }

    if (usersetNamespace != null && !usersetNamespace.isEmpty()) {
      filterParams.put("userset_namespace", usersetNamespace);
    }

    if (usersetObject != null && !usersetObject.isEmpty()) {
      filterParams.put("userset_object", usersetObject);
    }

    if (usersetRelations != null && !usersetRelations.isEmpty()) {
      filterParams.put("userset_relation", String.join(",", usersetRelations));
    }

    AclFilter.applyFilterForUsers(filterParams, filter);

    Page<AclEntity> data = repository.findAll(filter, pageRequest);
    List<Acl> acls = data.getContent().stream().map(AclEntity::toAcl).collect(Collectors.toList());

    return PageView.<Acl>builder()
        .data(acls)
        .currentPage(page)
        .total(data.getTotalElements())
        .pages(data.getTotalPages())
        .build();
  }

  public Acl findOneById(String id) {
    return repository.findById(id).map(AclEntity::toAcl).orElse(null);
  }

  public Set<Acl> findAllByPrincipalAndNsObjectIn(String principal, List<String> nsObjects) {
    Set<AclEntity> usersetAcls = repository.findAllByNsobjectInAndUser(nsObjects, "*");
    Set<AclEntity> userAcls = repository.findAllByUser(principal);
    return Stream.concat(usersetAcls.stream(), userAcls.stream())
        .map(AclEntity::toAcl)
        .collect(Collectors.toSet());
  }

  public Set<Acl> findAllByPrincipal(String principal) {
    Set<AclEntity> userAcls = repository.findAllByUser(principal);
    return userAcls.stream().map(AclEntity::toAcl).collect(Collectors.toSet());
  }

  public Set<Acl> findAllByNsObjectIn(List<String> nsObjects) {
    Set<AclEntity> usersetAcls = repository.findAllByNsobjectInAndUser(nsObjects, "*");
    return usersetAcls.stream().map(AclEntity::toAcl).collect(Collectors.toSet());
  }

  public Set<String> findAllEndUsers() {
    return repository.findDistinctEndUsers();
  }

  public Set<String> findAllNamespaces() {
    return repository.findDistinctNamespaces();
  }

  public Set<String> findAllObjects() {
    return repository.findDistinctObjects();
  }

  public Set<Acl> findAllForCache(
      String usersetNamespace, String usersetObject, String usersetRelation) {
    Set<AclEntity> usersetAcls =
        repository.findAllByUsersetNamespaceAndUsersetObjectAndUsersetRelationAndUser(
            usersetNamespace, usersetObject, usersetRelation, "*");
    return usersetAcls.stream().map(AclEntity::toAcl).collect(Collectors.toSet());
  }

  public Long findMaxAclUpdatedByPrincipal(String principal) {
    return repository.findMaxAclUpdatedByPrincipal(principal);
  }

  public Set<Acl> findAllByNamespaceAndObjectAndUser(String namespace, String object, String user) {
    Set<AclEntity> usersetAcls =
        repository.findAllByNsobjectAndUser(String.format("%s:%s", namespace, object), "*");
    Set<AclEntity> userAcls =
        repository.findAllByNsobjectAndUser(String.format("%s:%s", namespace, object), user);
    return Stream.concat(usersetAcls.stream(), userAcls.stream())
        .map(AclEntity::toAcl)
        .collect(Collectors.toSet());
  }

  public void save(Acl acl) {
    AclEntity entity =
        AclEntity.builder()
            .id(acl.getId().toString())
            .nsobject(String.format("%s:%s", acl.getNamespace(), acl.getObject()))
            .namespace(acl.getNamespace())
            .object(acl.getObject())
            .relation(acl.getRelation())
            .user((acl.hasUserset()) ? "*" : acl.getUser())
            .usersetNamespace(acl.getUsersetNamespace())
            .usersetObject(acl.getUsersetObject())
            .usersetRelation(acl.getUsersetRelation())
            .build();

    repository.save(entity);
  }

  public void deleteById(String id) {
    repository.deleteById(id);
  }

  @Transactional
  public void delete(
      String namespace,
      String object,
      String relation,
      String user,
      String usersetNamespace,
      String usersetObject,
      String usersetRelation) {
    repository
        .deleteAllByNamespaceAndObjectAndRelationAndUserAndUsersetNamespaceAndUsersetObjectAndUsersetRelation(
            namespace, object, relation, user, usersetNamespace, usersetObject, usersetRelation);
  }

  @Transactional
  public void delete(Acl acl) {
    delete(
        acl.getNamespace(),
        acl.getObject(),
        acl.getRelation(),
        acl.getUser(),
        acl.getUsersetNamespace(),
        acl.getUsersetObject(),
        acl.getUsersetRelation());
  }
}
