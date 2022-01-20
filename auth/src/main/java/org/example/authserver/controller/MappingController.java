package org.example.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.entity.MappingEntityList;
import org.example.authserver.repo.pgsql.MappingRepository;
import org.example.authserver.service.zanzibar.MappingService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/mapping")
public class MappingController {

    private final MappingRepository repository;

    private final MappingService mappingService;

    public MappingController(MappingRepository repository, MappingService mappingService) {
        this.repository = repository;
        this.mappingService = mappingService;
    }

    @GetMapping("/list")
    public List<MappingEntity> listAcl(){
        return repository.findAll();
    }

    @PostMapping("/create")
    public void addMapping(@Valid @RequestBody MappingEntity dto){
        log.info("Created Mapping: {}", dto);
        dto.setId(UUID.randomUUID().toString());
        repository.save(dto);
    }

    @PostMapping("/create-many")
    public void addMappings(@Valid @RequestBody MappingEntityList dto){
        for (MappingEntity entity : dto.getMappings()){
            addMapping(entity);
        }
    }

    @DeleteMapping("/clear")
    public void clearMappings(){
        log.info("Delete Mappings");
        repository.deleteAll();
    }

//    @DeleteMapping("/delete/{id}")
//    public void deleteAcl(@PathVariable String id){
//        log.info("Delete Mapping: {}", id);
//        repository.delete(id);
//    }


    @DeleteMapping("/delete/{id}")
    public void deleteMapping(@PathVariable String id){
        log.info("Delete Mapping: {}", id);
        repository.deleteById(id);
    }

    @GetMapping("/refresh-cache")
    public void refreshCache(){
        mappingService.refreshCache();
    }

}
