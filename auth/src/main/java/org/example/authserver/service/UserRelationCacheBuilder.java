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

    public boolean isInProgress() {
        return !inProgressUsers.isEmpty();
    }

    public void firstTimeBuildAsync() {
        Executors.newSingleThreadExecutor().submit(this::firstTimeBuild);
    }

    public boolean firstTimeBuild() {
        boolean isFirstTime = userRelationRepository.count() == 0;
        if (!isFirstTime) {
            return false;
        }

        return buildAll();
    }

    public boolean buildAll() {
        if (!this.config.isEnabled()) {
            log.warn("User relations cache is not enabled.");
            return false;
        }

        if (isInProgress()) {
            log.warn("Build process is already in progress. Skip.");
            return false;
        }

        Stopwatch started = Stopwatch.createStarted();
        log.info("Building all user relations...");

        Set<String> endUsers = aclRepository.findAllEndUsers();
        log.info("Found {} end users for building relations cache.", endUsers.size());
        if (endUsers.isEmpty()) {
            return false;
        }

        Set<String> namespaces = aclRepository.findAllNamespaces();
        log.info("Found {} namespaces for building relations cache.", namespaces.size());
        if (namespaces.isEmpty()) {
            log.warn("Unable to find namespaces. Skip building all cache.");
            return false;
        }

        Set<String> objects = aclRepository.findAllObjects();
        log.info("Found {} objects for building relations cache.", objects.size());
        if (objects.isEmpty()) {
            log.warn("Unable to find objects. Skip building all cache.");
            return false;
        }

        inProgressUsers.addAll(endUsers);

        for (String endUser : endUsers) {
            buildUserRelations(endUser, namespaces, objects);
        }
        inProgressUsers.clear();

        log.info("All user relations are built successfully. {}ms", started.elapsed(TimeUnit.MILLISECONDS));
        return true;
    }

    public boolean build(String user) {
        if (!this.config.isEnabled() || !this.config.isUpdateOnAclChange()) {
            log.trace("User relations cache update is skipped. Enabled: {}, UpdateOnAclChange: {}", config.isEnabled(), config.isUpdateOnAclChange());
            return false;
        }

        if (inProgressUsers.contains(user)) {
            log.warn("Building for user {} is already in progress. Scheduled update for later.", user);
            scheduledUsers.add(user);
            return false;
        }

        inProgressUsers.add(user);

        buildUserRelations(user);

        inProgressUsers.remove(user);
        scheduledUsers.remove(user);
        return true;
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

    public void buildUserRelations(String user, Set<String> namespaces, Set<String> objects) {
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

        log.trace("Found {} relations for user {}", relations.size(), user);

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
        if (isInProgress()) {
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

    public boolean hasScheduled(String user) {
        return scheduledUsers.contains(user);
    }

    public boolean hasInProgress(String user) {
        return inProgressUsers.contains(user);
    }

    public boolean canUseCache(String user) {
        return config.isEnabled() && !hasScheduled(user) && !hasInProgress(user);
    }
}
