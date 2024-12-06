package org.example.authserver.controller;

import authserver.acl.Acl;
import authserver.common.AclOperation;
import authserver.common.AclOperationDto;
import com.google.common.base.Stopwatch;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.AclsRequestDTO;
import org.example.authserver.entity.PageView;
import org.example.authserver.repo.SubscriptionRepository;
import org.example.authserver.service.AclService;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.SplitTestService;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/acl")
public class AclController {

  private final AclService repository;
  private final SubscriptionRepository subscriptionRepository;
  private final CacheService cacheService;
  private final SplitTestService splitTestService;
  private final AppProperties appProperties;

  public AclController(
      AclService repository,
      SubscriptionRepository subscriptionRepository,
      CacheService cacheService,
      SplitTestService splitTestService,
      AppProperties appProperties) {
    this.repository = repository;
    this.subscriptionRepository = subscriptionRepository;
    this.cacheService = cacheService;
    this.splitTestService = splitTestService;
    this.appProperties = appProperties;
  }

  @GetMapping("/list")
  public PageView<Acl> listAcl(
      @RequestParam(required = false) String namespace,
      @RequestParam(required = false) String object,
      @RequestParam(required = false) List<String> relations,
      @RequestParam(required = false) String user,
      @RequestParam(name = "userset_namespace", required = false) String usersetNamespace,
      @RequestParam(name = "userset_object", required = false) String usersetObject,
      @RequestParam(name = "userset_relation", required = false) List<String> usersetRelations,
      @RequestParam(value = "sort", required = false) String sort,
      @RequestParam(value = "direction", required = false) Sort.Direction direction,
      @Min(value = 1, message = "min: 1") @RequestParam(value = "page", defaultValue = "1")
          Integer page,
      @Max(value = 100, message = "max: 100") @RequestParam(value = "pageSize", defaultValue = "50")
          Integer pageSize) {
    return repository.findAll(
        namespace,
        object,
        relations,
        user,
        usersetNamespace,
        usersetObject,
        usersetRelations,
        sort,
        direction,
        page,
        pageSize);
  }

  @PostMapping("/create")
  public void createAcl(@Valid @RequestBody Acl acl) {
    cacheService.purgeCacheAsync(acl.getUser(), acl.getCreated());
    createAcl_(acl);
  }

  @PostMapping("/create_multiple")
  public void createMultiAcl(@Valid @RequestBody AclsRequestDTO multiAcl) {
    Set<String> usersProcessed = new HashSet<>();
    for (Acl acl : multiAcl.getAcls()) {
      if (!usersProcessed.contains(acl.getUser())) {
        cacheService.purgeCacheAsync(acl.getUser(), acl.getCreated());
        usersProcessed.add(acl.getUser());
      }
      createAcl_(acl);
    }
  }

  private void createAcl_(Acl acl) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    log.info("Creating ACL: {}", acl);
    List<String> relations = acl.getRelation() != null ? List.of(acl.getRelation()) : null;
    List<String> usersetRelations =
        acl.getUsersetRelation() != null ? List.of(acl.getUsersetRelation()) : null;
    PageView<Acl> existingAcls =
        repository.findAll(
            acl.getNamespace(),
            acl.getObject(),
            relations,
            acl.getUser(),
            acl.getUsersetNamespace(),
            acl.getUsersetObject(),
            usersetRelations,
            null,
            Sort.Direction.ASC,
            1,
            1);
    if (existingAcls.getTotal() > 0) {
      log.info(
          "Skipping acl {}, existing aclId {}",
          acl,
          new ArrayList<>(existingAcls.getData()).get(0).getId());
      return;
    }
    repository.save(acl);
    subscriptionRepository.publish(acl);
    if (appProperties.isCopyModeEnabled()) {
      splitTestService.submitAsync(
          AclOperationDto.builder().op(AclOperation.CREATE).acl(acl).build());
    }
    log.info("Created ACL: {}, time {}ms", acl, stopwatch.elapsed(TimeUnit.MILLISECONDS));
  }

  @PostMapping("/delete")
  public void deleteAcl(@Valid @RequestBody Acl acl) {
    log.info("Delete acl: {}", acl);
    cacheService.purgeCacheAsync(acl.getUser(), acl.getCreated());
    repository.delete(acl);
    if (appProperties.isCopyModeEnabled()) {
      splitTestService.submitAsync(AclOperationDto.builder().op(AclOperation.DEL).acl(acl).build());
    }
  }
}
