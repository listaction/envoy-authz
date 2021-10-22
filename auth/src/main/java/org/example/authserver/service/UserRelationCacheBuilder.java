package org.example.authserver.service;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.authserver.config.UserRelationsConfig;
import org.example.authserver.entity.UserRelationEntity;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.zanzibar.Zanzibar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
public class UserRelationCacheBuilder {

    private final static ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2);

    private final UserRelationsConfig config;
    private final Zanzibar zanzibar;
    private final AclRepository aclRepository;
    private final UserRelationRepository userRelationRepository;

    private final List<String> inProgressUsers = new CopyOnWriteArrayList<>();
    private final List<String> scheduledUsers = new CopyOnWriteArrayList<>();

    public UserRelationCacheBuilder(UserRelationsConfig config, AclRepository aclRepository, UserRelationRepository userRelationRepository, Zanzibar zanzibar) {
        this.config = config;
        this.aclRepository = aclRepository;
        this.userRelationRepository = userRelationRepository;
        this.zanzibar = zanzibar;

        EXECUTOR.scheduleAtFixedRate(this::scheduledBuild, 0, config.getScheduledPeriodTime(), config.getScheduledPeriodTimeUnit());
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

        Set<String> namespaces = aclRepository.findAllNamespaces();
        if (namespaces.isEmpty()) {
            log.warn("Unable to find namespaces. Skip building all cache.");
            return;
        }

        Set<String> objects = aclRepository.findAllObjects();
        if (objects.isEmpty()) {
            log.warn("Unable to find objects. Skip building all cache.");
            return;
        }

        inProgressUsers.addAll(endUsers);

        for (String endUser : endUsers) {
            buildUserRelations(endUser, namespaces, objects);
        }
        inProgressUsers.clear();

        log.info("All user relations are built successfully. {}ms", started.elapsed(TimeUnit.MILLISECONDS));
    }

    public void build(String user) {
        if (!this.config.isEnabled() || !this.config.isUpdateOnAclChange()) {
            log.trace("User relations cache update is skipped. Enabled: {}, UpdateOnAclChange: {}", config.isEnabled(), config.isUpdateOnAclChange());
            return;
        }

        if (inProgressUsers.contains(user)) {
            log.warn("Building for user {} is already in progress. Scheduled update for later.", user);
            scheduledUsers.add(user);
            return;
        }

        inProgressUsers.add(user);
        buildUserRelations(user);
        inProgressUsers.remove(user);
        scheduledUsers.remove(user);
    }

    private void buildUserRelations(String user) {
        Set<String> namespaces = aclRepository.findAllNamespaces();
        if (namespaces.isEmpty()) {
            log.warn("Unable to find namespaces. Skip building cache for user: {}", user);
            return;
        }

        Set<String> objects = aclRepository.findAllObjects();
        if (objects.isEmpty()) {
            log.warn("Unable to find objects. Skip building cache for user: {}", user);
            return;
        }

        buildUserRelations(user, namespaces, objects);
    }

    private void buildUserRelations(String user, Set<String> namespaces, Set<String> objects) {
        if (StringUtils.isBlank(user) || "*".equals(user)) {
            log.trace("Skip building cache for user: {}", user);
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        log.trace("Building user relations cache for user {} ...", user);

        Set<String> relations = new HashSet<>();
        for (String namespace : namespaces) {
            for (String object : objects) {
                relations.addAll(zanzibar.getRelations(namespace, object, user, new HashMap<>(), new HashMap<>()));
            }
        }

        log.info("Found {} relations for user {}", relations.size(), user);

        userRelationRepository.save(UserRelationEntity.builder()
                .user(user)
                .relations(relations)
                .build());

        log.trace("Finished building user relations cache for user {}, time: {}", user, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    private void scheduledBuild() {
        if (scheduledUsers.isEmpty()) {
            return;
        }

        for (String user : new HashSet<>(scheduledUsers)) {
            build(user);
        }
    }

    public boolean fullRebuildAsync() {
        if (!inProgressUsers.isEmpty()) {
            log.warn("Build process is already in progress. Skip.");
            return false;
        }
        EXECUTOR.execute(() -> {
            userRelationRepository.deleteAll();
            buildAll();
        });
        log.info("Scheduled full rebuild.");
        return true;
    }

    public boolean buildAsync(String user) {
        EXECUTOR.execute(() -> build(user));
        log.info("Scheduled updated for user {}.", user);
        return true;
    }
}
