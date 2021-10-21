package org.example.authserver.service;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.authserver.config.UserRelationsConfig;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.zanzibar.Zanzibar;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UserRelationCacheBuilder {

    private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final UserRelationsConfig config;
    private final Zanzibar zanzibar;
    private final AclRepository aclRepository;
    private final UserRelationRepository userRelationRepository;

    private final List<String> inProgressUsers = new CopyOnWriteArrayList<>();

    public UserRelationCacheBuilder(UserRelationsConfig config, AclRepository aclRepository, UserRelationRepository userRelationRepository, Zanzibar zanzibar) {
        this.config = config;
        this.aclRepository = aclRepository;
        this.userRelationRepository = userRelationRepository;
        this.zanzibar = zanzibar;
    }

    public void firstTimeBuildAsync() {
        Executors.newSingleThreadExecutor().submit(this::firstTimeBuild);
    }

    public void firstTimeBuild() {
        boolean isFirstTime = userRelationRepository.count() == 0;
        if (!isFirstTime) {
            return;
        }

        buildAll();
    }

    public void buildAll() {
        if (!this.config.isEnabled()) {
            log.warn("User relations cache is not enabled.");
            return;
        }

        if (!inProgressUsers.isEmpty()) {
            log.warn("Build process is already in progress. Skip.");
            return;
        }

        Stopwatch started = Stopwatch.createStarted();
        log.info("Building all user relations...");

        Set<String> endUsers = aclRepository.findAllEndUsers();
        log.info("Found {} end users for building relations cache.", endUsers.size());
        if (endUsers.isEmpty()) {
            return;
        }

        inProgressUsers.addAll(endUsers);
        for (String endUser : endUsers) {
            buildUserRelations(endUser);
        }
        inProgressUsers.clear();

        log.info("All user relations are built successfully. {}ms", started.elapsed(TimeUnit.MILLISECONDS));
    }

    public void update(String user) {
        if (!this.config.isEnabled() || !this.config.isUpdateOnAclChange()) {
            log.trace("User relations cache update is skipped. Enabled: {}, UpdateOnAclChange: {}", config.isEnabled(), config.isUpdateOnAclChange());
            return;
        }

        if (inProgressUsers.contains(user)) {
            log.warn("Building for user {} is already in progress.", user);
            return;
        }

        inProgressUsers.add(user);
        buildUserRelations(user);
        inProgressUsers.remove(user);
    }

    private void buildUserRelations(String user) {
        if (StringUtils.isBlank(user) || "*".equals(user)) {
            log.trace("Skip building cache for user: {}", user);
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.trace("Building user relations cache for user {} ...", user);



        log.trace("Finished building user relations cache for user {}, time: {}", user, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
