package org.example.authserver.service;

import authserver.acl.AclRelationConfig;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.zanzibar.AclRelationConfigRepository;
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

    public CacheLoaderServiceImpl(CacheService cacheService, AclRelationConfigRepository configRepository, AclRelationConfigService configService) {
        this.cacheService = cacheService;
        this.configRepository = configRepository;
        this.configService = configService;
    }

    @Override
    public void subscribe() {
        configRepository.subscribe()
                .doOnNext(config->updateConfigs())
                .subscribeOn(Schedulers.parallel())
                .subscribe();

        updateConfigs();
    }

    public void updateConfigs() {
        log.info("updateConfigs started");
        Map<String, AclRelationConfig> configMap = configRepository.findAll().stream()
                .collect(Collectors.toMap(AclRelationConfig::getNamespace, m->m));
        cacheService.updateConfigs(configMap);
        configService.update();
        log.info("updateConfigs finished");
    }

}
