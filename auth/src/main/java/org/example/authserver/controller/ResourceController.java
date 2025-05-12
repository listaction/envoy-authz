package org.example.authserver.controller;

import authserver.acl.Acl;
import authserver.common.AclOperation;
import authserver.common.AclOperationDto;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.ResourceDto;
import org.example.authserver.entity.ResourcePermissionsDto;
import org.example.authserver.repo.SubscriptionRepository;
import org.example.authserver.service.AclService;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.ResourceService;
import org.example.authserver.service.SplitTestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/acl/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final AclService aclService;
    private final SubscriptionRepository subscriptionRepository;
    private final SplitTestService splitTestService;
    private final CacheService cacheService;
    private final AppProperties appProperties;

    public ResourceController(ResourceService resourceService, AclService aclService, SubscriptionRepository subscriptionRepository, SplitTestService splitTestService, CacheService cacheService, AppProperties appProperties) {
        this.resourceService = resourceService;
        this.aclService = aclService;
        this.subscriptionRepository = subscriptionRepository;
        this.splitTestService = splitTestService;
        this.cacheService = cacheService;
        this.appProperties = appProperties;
    }

    @GetMapping
    public List<ResourceDto> listResources(@RequestParam String namespace) {
        return resourceService.findAllByNamespace(namespace);
    }

    @PostMapping
    public void createResource(@Valid @RequestBody ResourcePermissionsDto resourcePermissions) {
        log.info("Creating resource with permissions: {}", resourcePermissions);
        
        ResourceDto resource = new ResourceDto(
            resourcePermissions.getNamespace(),
            resourcePermissions.getResourceId());

        resourceService.createResourceIfMissing(resource);

        // Process each user's permissions for this resource
        resourcePermissions.getPermissions().forEach((userId, permissions) -> {
            for (String permission : permissions) {

                Acl acl = new Acl();
                acl.setId(UUID.randomUUID());
                acl.setNamespace(resourcePermissions.getNamespace());
                acl.setObject(resourcePermissions.getResourceId());
                acl.setRelation(permission);
                acl.setUser(userId);

                aclService.save(acl);

                subscriptionRepository.publish(acl);
                if (appProperties.isCopyModeEnabled()) {
                    splitTestService.submitAsync(
                            AclOperationDto.builder().op(AclOperation.CREATE).acl(acl).build());
                }
            }
        });
    }

    @DeleteMapping("/{namespace}/{resourceId}")
    public void deleteResource(@PathVariable String namespace, @PathVariable String resourceId) {
        log.info("Delete resource: {}/{}", namespace, resourceId);

        List<ResourceDto> resources = resourceService.findAllByNamespaceAndResourceId(namespace, resourceId);
        Set<Acl> acls = aclService.findAllByNsObjectIn(List.of(String.format("%s:%s", namespace, resourceId)));
        Set<String> users = acls.stream().map(Acl::getUser).collect(java.util.stream.Collectors.toSet());

        for (Acl acl : acls) {
            aclService.delete(acl);
            if (appProperties.isCopyModeEnabled()) {
                splitTestService.submitAsync(
                        AclOperationDto.builder().op(AclOperation.DEL).acl(acl).build());
            }
        }

        for (String userId : users) {
            if ("*".equalsIgnoreCase(userId)) continue;
            cacheService.purgeCache(userId, System.currentTimeMillis());
        }
        resourceService.deleteAll(resources);
    }
}