package org.example.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/debug")
public class DebugController {

    private final Zanzibar zanzibar;

    public DebugController(Zanzibar zanzibar) {
        this.zanzibar = zanzibar;
    }

    @GetMapping("/relations")
    public Set<String> getRelations(@RequestParam String namespace, @RequestParam String object, @RequestParam String principal){
        log.info("get relations: {}:{} @ {}", namespace, object, principal);
        return zanzibar.getRelations(namespace, object, principal);
    }

    @GetMapping("/test")
    public boolean test(@RequestParam String namespace, @RequestParam String object, @RequestParam String relation, @RequestParam String principal){
        log.info("get relations: {}:{} @ {}", namespace, object, principal);
        return zanzibar.check(namespace, object, relation, principal);
    }

}
