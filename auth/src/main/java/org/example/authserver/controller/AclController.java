package org.example.authserver.controller;

import authserver.acl.Acl;
import com.google.common.base.Stopwatch;
import org.example.authserver.entity.AclsRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.SubscriptionRepository;
import org.example.authserver.service.CacheService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/acl")
public class AclController {

    private final AclRepository repository;
    private final SubscriptionRepository subscriptionRepository;
    private final CacheService cacheService;

    public AclController(AclRepository repository, SubscriptionRepository subscriptionRepository, CacheService cacheService) {
        this.repository = repository;
        this.subscriptionRepository = subscriptionRepository;
        this.cacheService = cacheService;
    }

    @GetMapping("/list")
    public Set<Acl> listAcl(){
        return repository.findAll();
    }

    @PostMapping("/create")
    public void createAcl(@Valid @RequestBody Acl acl){
        cacheService.purgeCacheAsync(acl.getUser(), acl.getCreated());
        createAcl_(acl);
    }

    @PostMapping("/create_multiple")
    public void createMultiAcl(@Valid @RequestBody AclsRequestDTO multiAcl){
        Set<String> usersProcessed = new HashSet<>();
        for (Acl acl : multiAcl.getAcls()){
            if (!usersProcessed.contains(acl.getUser())){
                cacheService.purgeCacheAsync(acl.getUser(), acl.getCreated());
                usersProcessed.add(acl.getUser());
            }
            createAcl_(acl);
        }
    }

    private void createAcl_(Acl acl){
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("Creating ACL: {}", acl);
        repository.save(acl);
        subscriptionRepository.publish(acl);
        log.info("Created ACL: {}, time {}ms", acl, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

//    @DeleteMapping("/delete/{id}")
//    public void deleteAcl(@PathVariable String id){
//        log.info("Delete Mapping: {}", id);
//        repository.delete(id);
//    }

}
