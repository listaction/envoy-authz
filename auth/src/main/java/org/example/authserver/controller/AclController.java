package org.example.authserver.controller;

import authserver.acl.Acl;
import org.example.authserver.entity.AclsRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.SubscriptionRepository;
import org.example.authserver.service.UserRelationsCacheService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/acl")
public class AclController {

    private final AclRepository repository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRelationsCacheService userRelationCacheService;

    public AclController(AclRepository repository, SubscriptionRepository subscriptionRepository, UserRelationsCacheService userRelationCacheService) {
        this.repository = repository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRelationCacheService = userRelationCacheService;
    }

    @GetMapping("/list")
    public Set<Acl> listAcl(){
        return repository.findAll();
    }

    @PostMapping("/create")
    public void createAcl(@Valid @RequestBody Acl acl){
        log.info("Created ACL: {}", acl);
        repository.save(acl);
        subscriptionRepository.publish(acl);
        userRelationCacheService.updateAsync(acl.getUser());
    }

    @PostMapping("/create_multiple")
    public void createMultiAcl(@Valid @RequestBody AclsRequestDTO multiAcl){
        for (Acl acl : multiAcl.getAcls()){
            createAcl(acl);
        }
    }


//    @DeleteMapping("/delete/{id}")
//    public void deleteAcl(@PathVariable String id){
//        log.info("Delete Mapping: {}", id);
//        repository.delete(id);
//    }

}
