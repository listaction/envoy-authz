package com.example.service;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping("/create")
    public String createContact(@RequestHeader("authorization") String authorization, @RequestBody Object body) throws IOException {
        log.info("Authorization: {}", authorization);
        String token = authorization.replace("Bearer ", "");
        String uuid = UUID.randomUUID().toString(); // contact id

        try {
            Acl acl1 = Acl.create(String.format("contact:%s#%s@%s", uuid, "owner", "group:contactusers#admin"));
            Acl acl2 = Acl.create(String.format("contact:%s#%s@%s", uuid, "editor", "group:contactusers#member"));

            api.addRule(acl1);
            api.addRule(acl2);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return uuid;
    }

    @PostMapping("/update")
    public String updateContact(@RequestHeader("authorization") String authorization, @RequestBody Object body){
        log.info("Authorization: {}", authorization);
        log.info("Update: {}", body);
        return "UPDATED";
    }

    @PostMapping("/read")
    public String getContact(@RequestHeader("authorization") String authorization, @RequestBody Object body){
        log.info("Authorization: {}", authorization);
        log.info("Get: {}", body);
        return "ContactData{...}";
    }

    @PostMapping("/delete")
    public String deleteContact(@RequestHeader("authorization") String authorization, @RequestBody Object body){
        log.info("Authorization: {}", authorization);
        log.info("Delete: {}", body);
        return "DELETED";
    }

}
