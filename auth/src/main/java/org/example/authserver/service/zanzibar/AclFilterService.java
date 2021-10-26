package org.example.authserver.service.zanzibar;

import com.google.common.base.Stopwatch;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.RelationsService;
import org.example.authserver.service.model.Mapping;
import org.example.authserver.service.model.RequestCache;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

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

        List<Mapping> mappings = mappingService.processRequest(request, claims);
        long time3 = System.currentTimeMillis();
        if (mappings == null || mappings.size() == 0) {
            return CheckResult.builder().mappingsPresent(false).result(false).build();
        }

        String user = claims.getSubject();
        RequestCache requestCache = cacheService.prepareHighCardinalityCache(user);

        Set<String> allowedTags = new HashSet<>();
        for (Mapping mapping : mappings) {
            String mappingId = mapping.get("aclId");
            String namespace = mapping.get("namespace");
            String object = mapping.get("object");

            Set<String> roles = mapping.parseRoles();
            if (roles.isEmpty()) {
                return CheckResult.builder().mappingsPresent(true).rejectedWithMappingId(mappingId).result(false).build();
            }

            Set<String> relations = requestCache.getPrincipalHighCardinalityCache().get(user);

            boolean r = false;
            if (HasTag(relations, roles, namespace, object)) {
                r = true;
            } else {
                Stopwatch relationsStopwatch = Stopwatch.createStarted();
                relations = relationsService.getRelations(namespace, object, user, requestCache);
                log.info("zanzibar.getRelations {} ms.", relationsStopwatch.elapsed(TimeUnit.MILLISECONDS));

                if (HasTag(relations, roles, namespace, object)) {
                    r = true;
                }
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
