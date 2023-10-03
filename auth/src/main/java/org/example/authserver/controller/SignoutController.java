package org.example.authserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.AuthService;
import org.example.authserver.service.RedisService;
import org.example.authserver.service.SignoutService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/signout")
public class SignoutController {

  private final SignoutService signoutService;

  private final AuthService authService;

  public SignoutController(
      SignoutService signoutService, AuthService authService, RedisService redis) {
    this.signoutService = signoutService;
    this.authService = authService;
  }

  @PostMapping("/{tenant}/{jti}/{expirationTime}")
  public void signout(
      @PathVariable String tenant, @PathVariable String jti, @PathVariable long expirationTime) {
    log.debug("signout");
    signoutService.signout(tenant, jti, expirationTime);
  }

  @PostMapping("/full/{tenant}/{userId}")
  public void fullSignout(@PathVariable String tenant, @PathVariable String userId) {
    log.debug("full signout");
    signoutService.fullSignout(tenant, userId);
  }

  @DeleteMapping("/full/{tenant}/{userId}")
  public void removeFullSignoutKey(@PathVariable String tenant, @PathVariable String userId) {
    log.debug("remove full signout key");
    authService.removeFullSignoutKey(userId);
  }
}
