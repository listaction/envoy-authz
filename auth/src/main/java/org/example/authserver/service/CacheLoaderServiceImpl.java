package org.example.authserver.service;

import authserver.acl.AclRelationConfig;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.example.authserver.repo.SubscriptionRepository;
import org.example.authserver.service.zanzibar.AclRelationConfigService;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CacheLoaderServiceImpl implements CacheLoaderService {

    private final CacheService cacheService;
    private final AclRelationConfigRepository configRepository;
    private final AclRelationConfigService configService;
    private final SubscriptionRepository subscriptionRepository;

    public CacheLoaderServiceImpl(CacheService cacheService, AclRelationConfigRepository configRepository, AclRelationConfigService configService, SubscriptionRepository subscriptionRepository) {
        this.cacheService = cacheService;
        this.configRepository = configRepository;
        this.configService = configService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public void subscribe() {
        subscriptionRepository.subscribeConfig()
                .doOnNext(this::updateConfigs)
                .subscribeOn(Schedulers.parallel())
                .subscribe();

        updateAllConfigs();
    }

    private void updateConfigs(String id) {
        log.info("updateConfigs [{}] started", id);
        AclRelationConfig config = configRepository.findOneById(id);
        cacheService.updateConfig(config);
        configService.update();
        log.info("updateConfigs [{}] finished", id);
    }

    public void updateAllConfigs() {
        log.info("updateAllConfigs started");
        Map<String, AclRelationConfig> configMap = configRepository.findAll().stream()
                .collect(Collectors.toMap(AclRelationConfig::getNamespace, m->m));
        cacheService.updateConfigs(configMap);
        configService.update();
        log.info("updateAllConfigs finished");
    }

}
