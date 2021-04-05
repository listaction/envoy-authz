package org.example.authserver.controller;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.SubscriptionRepository;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/acl")
public class AclController {

    private final AclRepository repository;
    private final SubscriptionRepository subscriptionRepository;

    public AclController(AclRepository repository, SubscriptionRepository subscriptionRepository) {
        this.repository = repository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/list")
    public Set<Acl> listAcl(){
        return repository.findAll();
    }

    @PostMapping("/create")
    public void addAcl(@Valid @RequestBody Acl acl){
        log.info("Created ACL: {}", acl);
        repository.save(acl);
        subscriptionRepository.publish(acl);
    }


//    @DeleteMapping("/delete/{id}")
//    public void deleteAcl(@PathVariable String id){
//        log.info("Delete Mapping: {}", id);
//        repository.delete(id);
//    }

}
