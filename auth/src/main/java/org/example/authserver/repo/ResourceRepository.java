package org.example.authserver.repo;

import jakarta.validation.constraints.NotBlank;
import org.example.authserver.entity.ResourceDto;
import org.example.authserver.entity.ResourceEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ResourceRepository extends CrudRepository<ResourceEntity, String> {

  List<ResourceEntity> findAll();

  List<ResourceEntity> findAllByNamespace(String namespace);

  boolean existsByNamespaceAndResourceId(@NotBlank String namespace, @NotBlank String resourceId);

  List<ResourceDto> findAllByNamespaceAndResourceId(String namespace, String resourceId);

  void deleteByNamespaceAndResourceId(String namespace, String resourceId);
}
