package org.example.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.SignoutService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/signout")
public class SignoutController {

    private final SignoutService signoutService;

    public SignoutController(SignoutService signoutService) {
        this.signoutService = signoutService;
    }

    @PostMapping("/{tenant}/{jti}/{expirationTime}")
    public void signout(@PathVariable String tenant, @PathVariable String jti, @PathVariable long expirationTime) {
        signoutService.signout(tenant, jti, expirationTime);
    }
}
