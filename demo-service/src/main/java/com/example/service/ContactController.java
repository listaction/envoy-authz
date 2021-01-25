package com.example.service;

import lombok.extern.slf4j.Slf4j;
import authserver.acl.Acl;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/contact")
public class ContactController {

    private final AclApi api;

    public ContactController(AclApi api) {
        this.api = api;
    }

    @PostMapping("/")
    public String createContact(@RequestHeader("authorization") String authorization) throws IOException {
        log.info("Authorization: {}", authorization);
        String token = authorization.replace("Bearer ", "");
        String uuid = UUID.randomUUID().toString(); // contact id
        Acl acl1 = Acl.create(String.format("contact:%s#%s@%s", uuid, "owner", token));
        Acl acl2 = Acl.create(String.format("contact:%s#%s@%s", uuid, "editor", String.format("group:contactusers#member", token)));
        Acl acl3 = Acl.create(String.format("contact:%s#%s@%s", uuid, "viewer", String.format("group:contactusers#member", token)));
        api.addRule(acl1);
        api.addRule(acl2);
        api.addRule(acl3);
        return uuid;
    }

    @PutMapping("/{contactId}")
    public String updateContact(@RequestHeader("authorization") String authorization, String contactId){
        log.info("Authorization: {}", authorization);
        log.info("Update: {}", contactId);
        return "UPDATED";
    }

    @GetMapping("/{contactId}")
    public String getContact(@RequestHeader("authorization") String authorization, String contactId){
        log.info("Authorization: {}", authorization);
        log.info("Get: {}", contactId);
        return "ContactData{...}";
    }

    @DeleteMapping("/{contactId}")
    public String deleteContact(@RequestHeader("authorization") String authorization, String contactId){
        log.info("Authorization: {}", authorization);
        log.info("Delete: {}", contactId);
        return "DELETED";
    }

}
