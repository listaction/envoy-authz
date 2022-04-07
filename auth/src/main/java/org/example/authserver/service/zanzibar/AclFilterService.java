package org.example.authserver.service.zanzibar;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.RelationsService;
import org.example.authserver.service.model.LocalCache;
import org.example.authserver.service.model.Mapping;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AclFilterService {

    private final MappingService mappingService;
    private final TokenService tokenService;
    private final RelationsService relationsService;
    private final CacheService cacheService;

    public AclFilterService(RelationsService relationsService, MappingService mappingService, TokenService tokenService, CacheService cacheService) {
        this.mappingService = mappingService;
        this.tokenService = tokenService;
        this.relationsService = relationsService;
        this.cacheService = cacheService;
    }

    public CheckResult checkRequest(CheckRequest request) {
        long start = System.currentTimeMillis();
        Claims claims = tokenService.getAllClaimsFromRequest(request);
        long time2 = System.currentTimeMillis();

        if (claims == null) return CheckResult.builder().jwtPresent(false).result(false).build();

        String user = claims.getSubject();
        List<Mapping> mappings = mappingService.processRequest(request, claims);
        long time3 = System.currentTimeMillis();
        if (mappings == null || mappings.size() == 0) {
            log.debug("Unable to find mapping for user {}.", user);
            return CheckResult.builder().mappingsPresent(false).result(false).build();
        }

        LocalCache localCache = new LocalCache();
        Long maxAclUpdate = relationsService.getAclMaxUpdate(user);

        Set<String> allowedTags = new HashSet<>();
        for (Mapping mapping : mappings) {
            String mappingId = mapping.getVariable("aclId");
            String namespace = mapping.getVariable("namespace");
            String object = mapping.getVariable("object");
            String path = mapping.getMappingEntity().getPath();

            Set<String> roles = new HashSet<>(mapping.getRoles());
            if (roles.isEmpty()) {
                return CheckResult.builder().mappingsPresent(true).rejectedWithMappingId(mappingId).result(false).build();
            }

            long time4 = System.currentTimeMillis();
            Set<String> relations = new HashSet<>();
            //cacheService.getCachedRelations(user, namespace, object, path, maxAclUpdate);
            boolean r = false;
            if (HasTag(relations, roles, namespace, object)){
                r = true;
                allowedTags.addAll(relations);
                log.info("zanzibar.getRelations (cache) {} ms.", System.currentTimeMillis() - time4);
            } else {  // no actual cache exists
                relations = relationsService.getRelations(namespace, object, user, localCache);
                log.info("zanzibar.getRelations {} ms.", System.currentTimeMillis() - time4);

                if (HasTag(relations, roles, namespace, object)) {
                    r = true;
                    allowedTags.addAll(relations);
                }
                //cacheService.persistCacheAsync(user, relations, path, maxAclUpdate);
            }

            if (!r) {
                log.info("expected roles: {}:{} {}", namespace, object, roles);
                log.info("roles available for {}: {}", user, relations);
                long end = System.currentTimeMillis();
                log.info("checkRequest {} ms.", end - start);
                return CheckResult.builder().mappingsPresent(true).rejectedWithMappingId(mappingId).result(false).build();
            }
        }

        long end = System.currentTimeMillis();
        log.info("getAllClaimsFromRequest {} ms.", time2 - start);
        log.info("mappingService.processRequest {} ms.", time3 - time2);
        log.info("checkRequest {} ms.", end - start);
        log.info("mappings size: {}.", mappings.size());
        return CheckResult.builder().mappingsPresent(true).result(true).tags(allowedTags).build();
    }

    private static boolean HasTag(Set<String> relations, Set<String> roles, String namespace, String object) {
        if (relations == null || relations.isEmpty()) {
            return false;
        }

        for (String role : roles) {
            String currentTag = String.format("%s:%s#%s", namespace, object, role);
            boolean tagFound = relations.contains(currentTag);
            if (tagFound) {
                log.trace("Found tag: {}", currentTag);
                return true;
            }
        }
        return false;
    }
}
