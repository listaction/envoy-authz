package org.example.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.UserRelationsCacheService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/cache")
public class CacheController {

    private final UserRelationsCacheService userRelationsCacheService;

    public CacheController(UserRelationsCacheService userRelationsCacheService) {
        this.userRelationsCacheService = userRelationsCacheService;
    }

    @GetMapping("/user-relations/scheduled")
    public boolean scheduledUpdate() {
        log.info("Scheduling 'user-relations' cache full rebuild...");
        return userRelationsCacheService.updateScheduledAsync();
    }

    @GetMapping("/user-relations/single")
    public boolean singleUser(@RequestParam String user) {
        log.info("Scheduling 'user-relations' rebuild for user {}", user);
        return userRelationsCacheService.updateAsync(user);
    }
}
