package org.example.authserver.service;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.model.RequestCache;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class RelationsService {

    private final Zanzibar zanzibar;
    private final UserRelationsCacheService userRelationsCacheService;
    private final MeterService meterService;

    public RelationsService(Zanzibar zanzibar, UserRelationsCacheService userRelationsCacheService, MeterService meterService) {
        this.zanzibar = zanzibar;
        this.userRelationsCacheService = userRelationsCacheService;
        this.meterService = meterService;
    }

    @Timed(value = "relation.get", percentiles = {0.99, 0.95, 0.75})
    public Set<String> getRelations(String namespace, String object, String principal, RequestCache requestCache) {
        Optional<Set<String>> cachedRelations = userRelationsCacheService.getRelations(principal);
        if (cachedRelations.isPresent()) {
            log.trace("Return cached relations for user {}", principal);
            meterService.countHitsCache();
            return cachedRelations.get();
        }

        meterService.countHitsZanzibar();
        return zanzibar.getRelations(namespace, object, principal, requestCache);
    }
}
