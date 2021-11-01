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
    private final MeterRegistry meterRegistry;

    public RelationsService(Zanzibar zanzibar, UserRelationsCacheService userRelationsCacheService, MeterRegistry meterRegistry) {
        this.zanzibar = zanzibar;
        this.userRelationsCacheService = userRelationsCacheService;
        this.meterRegistry = meterRegistry;
    }

    @Timed(value = "relation.get", percentiles = {0.99, 0.95, 0.75})
    public Set<String> getRelations(String namespace, String object, String principal, RequestCache requestCache) {
        Optional<Set<String>> cachedRelations = userRelationsCacheService.getRelations(principal);
        if (cachedRelations.isPresent()) {
            log.trace("Return cached relations for user {}", principal);
            return cachedRelations.get();
        }

        return meterRegistry.timer("relation.zanzibar").record(() -> zanzibar.getRelations(namespace, object, principal, requestCache));
    }
}
