package org.example.authserver.service;

import authserver.acl.Acl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.UserRelationEntity;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserRelationsCacheService {

    @Getter
    private final boolean enabled;
    private final Map<String, UserRelationEntity> cache = new ConcurrentHashMap<>();
    private final UserRelationCacheBuilder builder;

    public UserRelationsCacheService(AppProperties appProperties, AclRepository aclRepository, UserRelationRepository userTagRepository, Zanzibar zanzibar) {
        this.enabled = appProperties.getUserRelationsCache().isEnabled();
        this.builder = new UserRelationCacheBuilder(appProperties.getUserRelationsCache(), aclRepository, userTagRepository, cache, zanzibar);
        this.builder.build();
    }

    public Set<String> getRelations(String user) {
        if (StringUtils.isBlank(user)) {
            return Collections.emptySet();
        }

        UserRelationEntity entity = cache.get(user);
        if (entity == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(entity.getRelations());
    }

    public void update(Acl acl) {
        this.builder.update(acl.getUser());
    }
}
