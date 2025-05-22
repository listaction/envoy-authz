package org.example.authserver.controller;

import authserver.acl.Acl;
import authserver.common.AclOperation;
import authserver.common.AclOperationDto;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.RoleDto;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.RoleService;
import org.example.authserver.service.SplitTestService;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/acl/roles")
public class RoleController {

    private final RoleService roleService;
    private final SplitTestService splitTestService;
    private final CacheService cacheService;
    private final AppProperties appProperties;

    public RoleController(RoleService roleService, SplitTestService splitTestService, CacheService cacheService, AppProperties appProperties) {
        this.roleService = roleService;
        this.splitTestService = splitTestService;
        this.cacheService = cacheService;
        this.appProperties = appProperties;
    }

    @GetMapping
    public Set<RoleDto> listRoles() {
        return roleService.findAll();
    }

    @PostMapping
    public void createRole(@Valid @RequestBody RoleDto role) {
        log.info("Creating RoleDto: {}", role);
        roleService.save(role);
    }

    @DeleteMapping("/{roleId}")
    public void deleteRole(@PathVariable String roleId) {
        log.info("Delete role: {}", roleId);
        Set<Acl> deletedAcls = roleService.deleteById(roleId);
        Set<String> users = deletedAcls.stream()
                .map(Acl::getUser)
                .collect(Collectors.toSet());

        for (Acl acl : deletedAcls) {
            if (appProperties.isCopyModeEnabled()) {
                splitTestService.submitAsync(
                        AclOperationDto.builder().op(AclOperation.DEL).acl(acl).build());
            }
        }

        for (String userId : users){
            if ("*".equalsIgnoreCase(userId)) continue;
            cacheService.purgeCache(userId, System.currentTimeMillis());
        }
    }
}