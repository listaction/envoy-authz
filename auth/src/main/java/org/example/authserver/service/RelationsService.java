package org.example.authserver.service;

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

    public RelationsService(Zanzibar zanzibar, UserRelationsCacheService userRelationsCacheService) {
        this.zanzibar = zanzibar;
        this.userRelationsCacheService = userRelationsCacheService;
    }

    public Set<String> getRelations(String namespace, String object, String principal, RequestCache requestCache) {
        Optional<Set<String>> cachedRelations = userRelationsCacheService.getRelations(principal);
        if (cachedRelations.isPresent()) {
            log.trace("Return cached relations for user {}", principal);
            return cachedRelations.get();
        }

        return zanzibar.getRelations(namespace, object, principal, requestCache);
    }
}
