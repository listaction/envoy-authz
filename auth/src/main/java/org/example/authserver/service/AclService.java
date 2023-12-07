package org.example.authserver.service;

import authserver.acl.Acl;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.AclEntity;
import org.example.authserver.entity.PageView;
import org.example.authserver.repo.AclRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AclService {

  private final AclRepository repository;

  public AclService(AclRepository repository) {
    this.repository = repository;
  }

  public PageView<Acl> findAll(String namespace, String object, List<String> relations, Integer pageNum, Integer pageSize) {
    PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
    //String nsobject = String.format("%s:%s", namespace, object);
    //Page<AclEntity> page = repository.findByNamespaceLikeAndObjectLikeAndRelationIn(namespace, object, relations, pageRequest);
    Page<AclEntity> page = repository.findByNsobjectLike(namespace, pageRequest);
    Set<Acl> acls = page.getContent()
            .stream().map(AclEntity::toAcl).collect(Collectors.toSet());

    long count = repository.countByNamespaceLikeAndObjectLikeAndRelationIn(namespace, object, relations);

    return PageView.<Acl>builder()
            .data(acls)
            .currentPage(pageNum)
            .pages(PageView.calculateTotalPages(count, pageSize))
            .total(count)
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
