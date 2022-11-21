package org.example.authserver.controller;

import authserver.acl.Acl;
import authserver.common.AclOperation;
import authserver.common.AclOperationDto;
import com.google.common.base.Stopwatch;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.AclsRequestDTO;
import org.example.authserver.repo.SubscriptionRepository;
import org.example.authserver.service.AclService;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.SplitTestService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/acl")
public class AclController {

  private final AclService repository;
  private final SubscriptionRepository subscriptionRepository;
  private final CacheService cacheService;
  private final SplitTestService splitTestService;

  public AclController(
      AclService repository,
      SubscriptionRepository subscriptionRepository,
      CacheService cacheService,
      SplitTestService splitTestService) {
    this.repository = repository;
    this.subscriptionRepository = subscriptionRepository;
    this.cacheService = cacheService;
    this.splitTestService = splitTestService;
  }

  @GetMapping("/list")
  public Set<Acl> listAcl() {
    return repository.findAll();
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
    repository.save(acl);
    subscriptionRepository.publish(acl);
    splitTestService.submitAsync(
        AclOperationDto.builder().op(AclOperation.CREATE).acl(acl).build());
    log.info("Created ACL: {}, time {}ms", acl, stopwatch.elapsed(TimeUnit.MILLISECONDS));
  }

  @PostMapping("/delete")
  public void deleteAcl(@Valid @RequestBody Acl acl) {
    log.info("Delete acl: {}", acl);
    cacheService.purgeCacheAsync(acl.getUser(), acl.getCreated());
    repository.delete(acl);
    splitTestService.submitAsync(AclOperationDto.builder().op(AclOperation.DEL).acl(acl).build());
  }
}
