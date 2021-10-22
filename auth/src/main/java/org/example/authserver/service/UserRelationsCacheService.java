package org.example.authserver.service;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.UserRelationEntity;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserRelationsCacheService {

    private final UserRelationCacheBuilder builder;
    private final UserRelationRepository userRelationRepository;

    public UserRelationsCacheService(AppProperties appProperties, AclRepository aclRepository, UserRelationRepository userRelationRepository, Zanzibar zanzibar) {
        this.userRelationRepository = userRelationRepository;
        this.builder = new UserRelationCacheBuilder(appProperties.getUserRelationsCache(), aclRepository, userRelationRepository, zanzibar);
        this.builder.firstTimeBuildAsync(); // async to release bean creation
    }

    public Optional<Set<String>> getRelations(String user) {
        if (StringUtils.isBlank(user)) {
            return Optional.empty();
        }

        if (!builder.canUseCache(user)) {
            return Optional.empty();
        }

        Optional<UserRelationEntity> entity = userRelationRepository.findById(user);
        if (entity.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new HashSet<>(entity.get().getRelations()));
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
