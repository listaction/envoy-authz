package org.example.authserver.service.zanzibar;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

@Slf4j
@Service
@AllArgsConstructor
public class AclFilterService {
    private final MappingService mappingService;
    private final TokenService tokenService;
    private final RelationsService relationsService;
    private final CacheService cacheService;


    public CheckResult checkRequest(CheckRequest request) {
        Map<String, String> events = new HashMap<>();
        long start = System.nanoTime();
        Claims claims = tokenService.getAllClaimsFromRequest(request);
        long getAllClaimsFinished = System.nanoTime();

        if (Objects.isNull(claims)) {
            return CheckResult.builder().jwtPresent(false).result(false).events(events).build();
        }

        long processRequestStarted = System.nanoTime();
        String user = claims.getSubject();
        List<Mapping> mappings = mappingService.processRequest(request, claims);
        long processRequestStopped = System.nanoTime();

        if (CollectionUtils.isEmpty(mappings)) {
            events.put("Unable to find mapping for user", user);
            return CheckResult.builder().mappingsPresent(false).result(false).events(events).build();
        }

        Long maxAclUpdate = relationsService.getAclMaxUpdate(user); // found revision

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
                        .events(events)
                        .build();
            }

            long cacheOperationStarted = System.nanoTime();
            Set<String> relations = cacheService.getCachedRelations(user, namespace, object, path, maxAclUpdate); // get from cache

            if (checkTag(relations, roles, namespace, object)) { //cache hit
                allowedTags.addAll(relations);
                events.put("zanzibar.getCachedRelations in ms", toMs(cacheOperationStarted, System.nanoTime()));
            } else {                                             // cache miss
                relations = relationsService.getRelations(namespace, object, user, new LocalCache());
                events.put("zanzibar.getRelations ins ms", toMs(cacheOperationStarted, System.nanoTime()));

                if (checkTag(relations, roles, namespace, object)) {
                    allowedTags.addAll(relations);
                    events.put("cache warming", "");

                    cacheService.persistCacheAsync(user, relations, path, maxAclUpdate); // cache warming
                } else {
                    events.put(String.format("expected roles: %s:%s %s", namespace, object, roles), "");
                    events.put(String.format("roles available for %s: %s", user, relations), "");
                    events.put("CheckRequest in ms", toMs(start, System.nanoTime()));

                    return CheckResult.builder()
                            .mappingsPresent(true)
                            .rejectedWithMappingId(mappingId)
                            .result(false)
                            .events(events)
                            .build();
                }
            }
        }

        events.put("getAllClaimsFromRequest in ms.", toMs(start, getAllClaimsFinished));
        events.put("mappingService.processRequest in ms.", toMs(processRequestStarted, processRequestStopped));
        events.put("CheckRequest in ms", toMs(start, System.nanoTime()));
        events.put("mappings size: ", String.valueOf(mappings.size()));

        return CheckResult.builder()
                .mappingsPresent(true)
                .result(true)
                .tags(allowedTags)
                .events(events)
                .build();
    }

    private static String toMs(long nanosStart, long nanosFinish) {
        int nanosInMillis = 1000000;
        return String.valueOf((nanosFinish - nanosStart) / nanosInMillis);
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
}
