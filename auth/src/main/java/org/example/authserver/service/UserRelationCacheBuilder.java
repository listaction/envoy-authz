package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.authserver.config.UserRelationsConfig;
import org.example.authserver.entity.UserRelationEntity;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.zanzibar.Zanzibar;

import java.util.Map;

@Slf4j
public class UserRelationCacheBuilder {

    private UserRelationsConfig config;
    private final Zanzibar zanzibar;

    public UserRelationCacheBuilder(UserRelationsConfig config, AclRepository aclRepository, UserRelationRepository userTagRepository, Map<String, UserRelationEntity> cache, Zanzibar zanzibar) {
        this.config = config;
        this.zanzibar = zanzibar;
    }

    public void build() {
        if (!this.config.isEnabled()) {
            return;
        }

        // todo
    }

    public void update(String user) {
        if (!this.config.isEnabled() || !this.config.isUpdateOnAclChange()) {
            log.trace("User relations cache update is skipped. Enabled: {}, UpdateOnAclChange: {}", config.isEnabled(), config.isUpdateOnAclChange());
            return;
        }

        buildUserRelations(user);
    }

    private void buildUserRelations(String user) {
        if (StringUtils.isBlank(user) || "*".equals(user)) {
            log.trace("Skip building cache for user: {}", user);
            return;
        }

        // todo
    }
}
