package org.example.authserver.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.entity.MappingEntityList;
import org.example.authserver.repo.MappingRepository;
import org.example.authserver.service.MappingCacheService;
import org.example.authserver.service.zanzibar.MappingService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/mapping")
public class MappingController {

  private final MappingRepository repository;
  private final MappingService mappingService;
  private final MappingCacheService mappingCacheService;

  public MappingController(
      MappingRepository repository,
      MappingService mappingService,
      MappingCacheService mappingCacheService) {
    this.repository = repository;
    this.mappingService = mappingService;
    this.mappingCacheService = mappingCacheService;
  }

  @GetMapping("/list")
  public List<MappingEntity> listMappings() {
    return mappingService.findAll();
  }

  @PostMapping("/create")
  public void addMapping(@Valid @RequestBody MappingEntity mappingEntity) {
    log.info("Created Mapping: {}", mappingEntity);
    mappingService.create(mappingEntity);
  }

  @PostMapping("/create-many")
  public void addMappings(@Valid @RequestBody MappingEntityList dto) {
    for (MappingEntity entity : dto.getMappings()) {
      addMapping(entity);
    }
  }

  @DeleteMapping("/clear")
  public void clearMappings() {
    log.info("Delete Mappings");
    mappingService.deleteAll();
  }

  @DeleteMapping("/delete/{id}")
  public void deleteAcl(@PathVariable String id) {
    log.info("Delete Mapping by id: {}", id);
    repository.deleteById(id);
  }

  @GetMapping("/refresh-cache")
  public void notifyAllToRefreshCache() {
    mappingCacheService.notifyAllToRefreshCache();
  }
}
