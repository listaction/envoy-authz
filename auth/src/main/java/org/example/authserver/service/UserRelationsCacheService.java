package org.example.authserver.service;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.UserRelationEntity;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserRelationsCacheService {

    private final UserRelationCacheBuilder builder;
    private final UserRelationRepository userRelationRepository;
    private final AclRepository aclRepository;

    @Autowired
    public UserRelationsCacheService(AppProperties appProperties, AclRepository aclRepository, UserRelationRepository userRelationRepository, Zanzibar zanzibar, CacheService cacheService) {
        this(new UserRelationCacheBuilder(appProperties.getUserRelationsCache(), aclRepository, userRelationRepository, zanzibar, cacheService), userRelationRepository, aclRepository);
    }

    public UserRelationsCacheService(UserRelationCacheBuilder builder, UserRelationRepository userRelationRepository, AclRepository aclRepository) {
        this.userRelationRepository = userRelationRepository;
        this.aclRepository = aclRepository;
        this.builder = builder;
        this.builder.firstTimeBuildAsync(); // async to release bean creation
    }

    public Optional<Set<String>> getRelations(String user) {
        if (StringUtils.isBlank(user)) {
            return Optional.empty();
        }

        if (!builder.canUseCache(user)) {
            return Optional.empty();
        }

        Optional<UserRelationEntity> entityOptional = userRelationRepository.findById(user);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }

        UserRelationEntity entity = entityOptional.get();
        long maxAclUpdated = aclRepository.findMaxAclUpdatedByPrincipal(user);
        if (entity.getMaxAclUpdated() < maxAclUpdated) {
            log.warn("Can't use user relations cache, entity updatedAt: {}, maxAclUpdated: {}", entity.getMaxAclUpdated(), maxAclUpdated);
            return Optional.empty();
        }

        return Optional.of(new HashSet<>(entity.getRelations()));
    }

    public void update(Acl acl) {
        update(acl.getUser());
    }

    public void update(String user) {
        this.builder.build(user);
    }

    public boolean updateAllAsync() {
        return this.builder.fullRebuildAsync();
    }

    public boolean updateAsync(String user) {
        return this.builder.buildAsync(user);
    }
}
