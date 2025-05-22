package org.example.authserver.controller;

import authserver.acl.Acl;
import authserver.common.AclOperation;
import authserver.common.AclOperationDto;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.UserRolesDto;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.SplitTestService;
import org.example.authserver.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/acl/users")
public class UserController {

    private final UserService userService;
    private final SplitTestService splitTestService;
    private final CacheService cacheService;
    private final AppProperties appProperties;

    public UserController(UserService userService, SplitTestService splitTestService, CacheService cacheService, AppProperties appProperties) {
        this.userService = userService;
        this.splitTestService = splitTestService;
        this.cacheService = cacheService;
        this.appProperties = appProperties;
    }

    @PostMapping
    public void assignRolesToUser(@Valid @RequestBody UserRolesDto userRoles) {
        String userId = userRoles.getUserId();
        Set<Acl> acls = userService.assignRolesToUser(userId, userRoles.getRoles());
        for (Acl acl : acls) {
            if (appProperties.isCopyModeEnabled()) {
                splitTestService.submitAsync(
                        AclOperationDto.builder().op(AclOperation.CREATE).acl(acl).build());
            }
        }
    }

    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable String userId) {
        Set<Acl> deletedAcls = userService.deleteUser(userId);
        for (Acl acl : deletedAcls) {
            if (appProperties.isCopyModeEnabled()) {
                splitTestService.submitAsync(
                        AclOperationDto.builder().op(AclOperation.DEL).acl(acl).build());
            }
        }

        cacheService.purgeCache(userId, System.currentTimeMillis());
    }
}