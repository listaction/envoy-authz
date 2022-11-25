package org.example.authserver.service.zanzibar;

import authserver.common.CheckRequestDTO;
import com.newrelic.api.agent.Trace;
import io.jsonwebtoken.Claims;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.entity.LocalCache;
import org.example.authserver.entity.Mapping;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.RelationsService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class AclFilterService {
  private final MappingService mappingService;
  private final TokenService tokenService;
  private final RelationsService relationsService;
  private final CacheService cacheService;
  private final AppProperties appProperties;

  public AclFilterService(
      MappingService mappingService,
      TokenService tokenService,
      RelationsService relationsService,
      CacheService cacheService,
      AppProperties appProperties) {
    this.mappingService = mappingService;
    this.tokenService = tokenService;
    this.relationsService = relationsService;
    this.cacheService = cacheService;
    this.appProperties = appProperties;
  }

  @Trace
  public CheckResult checkRequest(CheckRequestDTO request) {
    Map<String, String> events = new HashMap<>();
    Map<String, Long> metrics = new HashMap<>();
    String httpMethod = request.getHttpMethod();
    String requestPath = request.getRequestPath();
    boolean cacheHit = false;

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

    long maxAclUpdate =
        (appProperties.isCacheEnabled())
            ? Optional.ofNullable(relationsService.getAclMaxUpdate(user)).orElse(0L)
            : 0L; // found revision

    Set<String> allowedTags = new HashSet<>();
    for (Mapping mapping : mappings) {
      String mappingId = mapping.getVariable("aclId");
      String namespace = mapping.getVariable("namespace");
      String object = mapping.getVariable("object");
      String path = mapping.getMappingEntity().getPath();

      Set<String> roles = new HashSet<>(mapping.getRoles());
      if (roles.isEmpty()) {
        return CheckResult.builder()
            .httpMethod(httpMethod)
            .requestPath(requestPath)
            .mappingsPresent(true)
            .rejectedWithMappingId(mappingId)
            .result(false)
            .events(events)
            .build();
      }

      long cacheOperationStarted = System.nanoTime();
      Set<String> relations =
          cacheService.getCachedRelations(
              user, namespace, object, path, maxAclUpdate); // get from cache

      if (checkTag(relations, roles, namespace, object)) { // cache hit
        allowedTags.addAll(relations);
        metrics.put(
            "zanzibar.getCachedRelations(ms)", toMs(cacheOperationStarted, System.nanoTime()));
        cacheHit = true;
      } else { // cache miss
        relations = relationsService.getRelations(namespace, object, user, new LocalCache());
        metrics.put("zanzibar.getRelations(ms)", toMs(cacheOperationStarted, System.nanoTime()));

        if (checkTag(relations, roles, namespace, object)) {
          allowedTags.addAll(relations);
          events.put("cache warming for user", user);
          cacheService.persistCacheAsync(user, relations, path, maxAclUpdate); // cache warming
        } else {
          events.put(String.format("expected roles: %s:%s %s", namespace, object, roles), "");
          events.put(String.format("roles available for %s: %s", user, relations), "");
          metrics.put("CheckRequest(ms)", toMs(start, System.nanoTime()));

          return CheckResult.builder()
              .httpMethod(httpMethod)
              .requestPath(requestPath)
              .mappingsPresent(true)
              .rejectedWithMappingId(mappingId)
              .result(false)
              .events(events)
              .metrics(metrics)
              .build();
        }
      }
    }

    metrics.put("getAllClaimsFromRequest(ms)", toMs(start, getAllClaimsFinished));
    metrics.put(
        "mappingService.processRequest(ms)", toMs(processRequestStarted, processRequestStopped));
    metrics.put("CheckRequest(ms)", toMs(start, System.nanoTime()));
    metrics.put("mappings size", (long) mappings.size());

    return CheckResult.builder()
        .httpMethod(httpMethod)
        .requestPath(requestPath)
        .mappingsPresent(true)
        .result(true)
        .tags(allowedTags)
        .events(events)
        .metrics(metrics)
        .cacheHit(cacheHit)
        .build();
  }

  private static long toMs(long nanosStart, long nanosFinish) {
    int nanosInMillis = 1000000;
    return (nanosFinish - nanosStart) / nanosInMillis;
  }

  private static boolean checkTag(
      Set<String> relations, Set<String> roles, String namespace, String object) {
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
