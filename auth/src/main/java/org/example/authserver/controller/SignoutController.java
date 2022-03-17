package org.example.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.RedisService;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nullable;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/signout")
public class SignoutController {

    private final RedisService redisService;

    private static final String SIGNOUT_REDIS_KEY = "%s__%s";

    public SignoutController(@Nullable JedisPool jedisPool) {
        this.redisService = new RedisService(jedisPool);
    }

    @GetMapping("/{tenant}/{jti}/{expirationTime}")
    private void signout(@PathVariable String tenant, @PathVariable String jti, @PathVariable long expirationTime) {
        try {
            String key = String.format(SIGNOUT_REDIS_KEY, tenant, jti);
            redisService.set(key, String.valueOf(expirationTime));
        } catch (Exception exception) {
            log.warn("Redis service is unavailable");
        }
    }
}
