package org.example.authserver.controller;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.repo.AclRepository;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/acl")
public class AclController {

    private final AclRepository repository;

    public AclController(AclRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/create")
    public void addAcl(@Valid @RequestBody Acl acl){
        log.info("Created ACL: {}", acl);
        repository.save(acl);
        repository.publish();
    }

}
