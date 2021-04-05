package org.example.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.pgsql.MappingRepository;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/mapping")
public class MappingController {

    private final MappingRepository repository;

    public MappingController(MappingRepository repository) {
        this.repository = repository;
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

    @DeleteMapping("/delete/{id}")
    public void deleteMapping(@PathVariable String id){
        log.info("Delete Mapping: {}", id);
        repository.deleteById(id);
    }

}
