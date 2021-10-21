package org.example.authserver.service.zanzibar;

import authserver.acl.Acl;
import com.google.common.base.Strings;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.UserRelationsCacheService;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.*;

@Slf4j
@Service
public class AclFilterService {

    private final Zanzibar zanzibar;
    private final MappingService mappingService;
    private final TokenService tokenService;
    private final UserRelationsCacheService userRelationsCacheService;

    public AclFilterService(Zanzibar zanzibar, MappingService mappingService, TokenService tokenService, UserRelationsCacheService userRelationsCacheService) {
        this.zanzibar = zanzibar;
        this.mappingService = mappingService;
        this.tokenService = tokenService;
        this.userRelationsCacheService = userRelationsCacheService;
  }

    public CheckResult checkRequest(CheckRequest request) {
        long start = System.currentTimeMillis();
        Claims claims = tokenService.getAllClaimsFromRequest(request);
        long time2 = System.currentTimeMillis();;
        if (claims == null) return CheckResult.builder().jwtPresent(false).result(false).build();

        List<Map<String, String>> mappings = mappingService.processRequest(request, claims);
        long time3 = System.currentTimeMillis();;
        if (mappings == null || mappings.size() == 0) {
            return CheckResult.builder().mappingsPresent(false).result(false).build();
        }

        Map<Tuple2<String, String>, Set<ZanzibarImpl.ExpandedAcl>> cache = new HashMap<>();
        Map<String, Set<Acl>> principalAclCache = new HashMap<>();
        Set<String> allowedTags = new HashSet<>();
        for (Map<String, String> variables : mappings) {
            String mappingId = variables.get("aclId");
            String mappingRoles = variables.getOrDefault("roles", "");
            List<String> mRoles;
            if (!Strings.isNullOrEmpty(mappingRoles)) {
                String[] tmp = mappingRoles.split(",");
                mRoles = Arrays.asList(tmp);
            } else {
                return CheckResult.builder().mappingsPresent(true).rejectedWithMappingId(mappingId).result(false).build();
            }

            boolean r = false;
            long time4 = System.currentTimeMillis();;
            Set<String> relations = getRelations(variables.get("namespace"), variables.get("object"), claims.getSubject(), cache, principalAclCache);
            long time5 = System.currentTimeMillis();;
            log.info("zanzibar.getRelations {} ms.", time5-time4);
            for (String role : mRoles) {
                String namespace = variables.get("namespace");
                String object = variables.get("object");
                log.trace("CHECKING: {}:{}#{}@{}", namespace, object, role, claims.getSubject());
                String currentTag = String.format("%s:%s#%s", namespace, object, role);
                boolean tagFound = relations.contains(currentTag);
                if (tagFound) r = true;
                allowedTags.addAll(relations);
            }
            if (!r) {
                log.info("expected roles: {}:{} {}", variables.get("namespace"), variables.get("object"), mRoles);
                log.info("roles available for {}: {}", claims.getSubject(), relations);
                long end = System.currentTimeMillis();
                log.info("checkRequest {} ms.", end - start);
                return CheckResult.builder().mappingsPresent(true).rejectedWithMappingId(mappingId).result(false).build();
            }
        }

        long end = System.currentTimeMillis();
        log.info("getAllClaimsFromRequest {} ms.", time2-start);
        log.info("mappingService.processRequest {} ms.", time3-time2);
        log.info("checkRequest {} ms.", end - start);
        log.info("mappings size: {}.", mappings.size());
        return CheckResult.builder().mappingsPresent(true).result(true).tags(allowedTags).build();
    }

    private Set<String> getRelations(String namespace, String object, String principal, Map<Tuple2<String, String>, Set<ZanzibarImpl.ExpandedAcl>> cache, Map<String, Set<Acl>> principalAclCache) {
        if (userRelationsCacheService.isEnabled()) {
            return userRelationsCacheService.getRelations(principal);
        }

        return zanzibar.getRelations(namespace, object, principal, cache, principalAclCache);
    }
}
