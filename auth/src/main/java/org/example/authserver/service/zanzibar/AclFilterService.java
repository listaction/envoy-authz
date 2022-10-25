package org.example.authserver.service.zanzibar;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.RelationsService;
import org.example.authserver.service.model.LocalCache;
import org.example.authserver.service.model.Mapping;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

@Slf4j
@Service
@AllArgsConstructor
public class AclFilterService {
    private final MappingService mappingService;
    private final TokenService tokenService;
    private final RelationsService relationsService;
    private final CacheService cacheService;

    private final static StopWatch stopWatch = new StopWatch("AclFilter");

    public CheckResult checkRequest(CheckRequest request) {
        stopWatch.start("getAllClaimsFromRequest");
        Claims claims = tokenService.getAllClaimsFromRequest(request);
        stopWatch.stop();

        if (Objects.isNull(claims)) {
            return CheckResult.builder().jwtPresent(false).result(false).build();
        }

        stopWatch.start("mappingService.processRequest");
        String user = claims.getSubject();
        List<Mapping> mappings = mappingService.processRequest(request, claims);
        stopWatch.stop();

        if (CollectionUtils.isEmpty(mappings)) {
            log.debug("Unable to find mapping for user {}.", user);
            return CheckResult.builder().mappingsPresent(false).result(false).build();
        }

        stopWatch.start("relationsService.getAclMaxUpdate");
        Long maxAclUpdate = relationsService.getAclMaxUpdate(user); // found revision
        stopWatch.stop();

        Set<String> allowedTags = new HashSet<>();
        for (Mapping mapping : mappings) {
            String mappingId = mapping.getVariable("aclId");
            String namespace = mapping.getVariable("namespace");
            String object = mapping.getVariable("object");
            String path = mapping.getMappingEntity().getPath();

            Set<String> roles = new HashSet<>(mapping.getRoles());
            if (roles.isEmpty()) {
                return CheckResult.builder()
                        .mappingsPresent(true)
                        .rejectedWithMappingId(mappingId)
                        .result(false)
                        .build();
            }

            stopWatch.start("zanzibar.getRelations - cache for " + mappingId);
            Set<String> relations = cacheService.getCachedRelations(user, namespace, object, path, maxAclUpdate); // get from cache
            stopWatch.stop();

            if (checkTag(relations, roles, namespace, object)) { //cache hit
                allowedTags.addAll(relations);
            } else {                                             // cache miss
                stopWatch.start("zanzibar.getRelations - for " + mappingId);
                relations = relationsService.getRelations(namespace, object, user, new LocalCache());

                if (checkTag(relations, roles, namespace, object)) {
                    allowedTags.addAll(relations);
                    cacheService.persistCacheAsync(user, relations, path, maxAclUpdate); // cache warming
                    stopWatch.stop();
                } else {
                    log.info("expected roles: {}:{} {}", namespace, object, roles);
                    log.info("roles available for {}: {}", user, relations);
                    stopWatch.stop();
                    log.info(timingsPrettyPrint());
                    return CheckResult.builder()
                            .mappingsPresent(true)
                            .rejectedWithMappingId(mappingId)
                            .result(false)
                            .build();
                }
            }
        }

        log.info("mappings size: {}.", mappings.size());
        log.info(timingsPrettyPrint());
        return CheckResult.builder()
                .mappingsPresent(true)
                .result(true)
                .tags(allowedTags)
                .build();
    }

    private static boolean checkTag(Set<String> relations, Set<String> roles, String namespace, String object) {
        if (CollectionUtils.isEmpty(relations)) {
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

    private static String timingsPrettyPrint() {
        return System.getProperty("line.separator")
                + "==============================" + System.getProperty("line.separator")
                + stopWatch.prettyPrint() + System.getProperty("line.separator") +
               "==============================";
    }
}
