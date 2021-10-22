package org.example.authserver.service;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.example.authserver.service.zanzibar.ZanzibarImpl;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.Map;
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

    public Set<String> getRelations(String namespace, String object, String principal, Map<Tuple2<String, String>, Set<ZanzibarImpl.ExpandedAcl>> cache, Map<String, Set<Acl>> principalAclCache) {
        Optional<Set<String>> cachedRelations = userRelationsCacheService.getRelations(principal);
        if (cachedRelations.isPresent()) {
            log.trace("Return cached relations for user {}", principal);
            return cachedRelations.get();
        }

        return zanzibar.getRelations(namespace, object, principal, cache, principalAclCache);
    }
}
