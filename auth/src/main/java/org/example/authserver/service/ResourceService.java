package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.ResourceDto;
import org.example.authserver.entity.ResourceEntity;
import org.example.authserver.repo.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<ResourceDto> findAllByNamespace(String namespace) {
        return resourceRepository.findAllByNamespace(namespace)
                .stream()
                .map(m->{
                    ResourceDto resourceDto = new ResourceDto();
                    resourceDto.setNamespace(m.getNamespace());
                    resourceDto.setResourceId(m.getResourceId());
                    return resourceDto;
                })
                .collect(Collectors.toList());
    }

    public void createResourceIfMissing(ResourceDto resource) {
        if (!resourceRepository.existsByNamespaceAndResourceId(resource.getNamespace(), resource.getResourceId())) {
            ResourceEntity entity = ResourceEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .namespace(resource.getNamespace())
                    .resourceId(resource.getResourceId())
                    .created(System.currentTimeMillis())
                    .updated(System.currentTimeMillis())
                    .build();

            resourceRepository.save(entity);
            log.info("Resource created: {}", resource);
        } else {
            log.info("Resource already exists: {}", resource);
        }
    }

    public List<ResourceDto> findAllByNamespaceAndResourceId(String namespace, String resourceId) {
        return resourceRepository.findAllByNamespaceAndResourceId(namespace, resourceId);
    }

    public void deleteAll(List<ResourceDto> resources) {
        for (ResourceDto resource : resources) {
            resourceRepository.deleteByNamespaceAndResourceId(resource.getNamespace(), resource.getResourceId());
        }
    }
}
