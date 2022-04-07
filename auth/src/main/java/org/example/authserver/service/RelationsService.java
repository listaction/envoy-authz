package org.example.authserver.service;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.service.model.LocalCache;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
public class RelationsService {

    private final Zanzibar zanzibar;
    private final AclRepository aclRepository;
    private final MeterService meterService;

    public RelationsService(Zanzibar zanzibar, AclRepository aclRepository, MeterService meterService) {
        this.zanzibar = zanzibar;
        this.aclRepository = aclRepository;
        this.meterService = meterService;
    }

    public Long getAclMaxUpdate(String principal){
        return aclRepository.findMaxAclUpdatedByPrincipal(principal);
    }

    @Timed(value = "relation.get", percentiles = {0.99, 0.95, 0.75})
    public Set<String> getRelations(String namespace, String object, String principal, LocalCache localCache){
        // todo: fix local cache
        meterService.countHitsZanzibar();
        return zanzibar.getRelations(namespace, object, principal, new LocalCache());
    }
}
