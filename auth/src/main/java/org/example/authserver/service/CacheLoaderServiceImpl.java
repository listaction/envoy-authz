package org.example.authserver.service;

import authserver.acl.AclRelationConfig;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.repo.SubscriptionRepository;
import org.example.authserver.service.zanzibar.AclRelationConfigService;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class CacheLoaderServiceImpl implements CacheLoaderService {

  private final CacheService cacheService;
  private final AclRelationConfigService configService;
  private final SubscriptionRepository subscriptionRepository;

  public CacheLoaderServiceImpl(
      CacheService cacheService,
      AclRelationConfigService configService,
      SubscriptionRepository subscriptionRepository) {
    this.cacheService = cacheService;
    this.configService = configService;
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public void subscribe() {
    subscriptionRepository
        .subscribeConfig()
        .doOnNext(this::updateConfigs)
        .subscribeOn(Schedulers.parallel())
        .subscribe();

    updateAllConfigs();
  }

  private void updateConfigs(String id) {
    log.info("updateConfigs [{}] started", id);
    AclRelationConfig config = configService.findOneById(id);
    cacheService.updateConfig(config);
    configService.update();
    log.info("updateConfigs [{}] finished", id);
  }

  public void updateAllConfigs() {
    log.info("updateAllConfigs started");
    Map<String, AclRelationConfig> configMap =
        configService.findAll().stream()
            .collect(Collectors.toMap(AclRelationConfig::getNamespace, m -> m));
    cacheService.updateConfigs(configMap);
    configService.update();
    log.info("updateAllConfigs finished");
  }
}
