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

  @PostMapping("/{tenant}/{userId}")
  public void userSignout(@PathVariable String tenant, @PathVariable String userId) {
    log.debug("user signout");
    signoutService.userSignout(tenant, userId);
  }

  @DeleteMapping("/{tenant}/{userId}")
  public void removeUserSignoutKey(@PathVariable String tenant, @PathVariable String userId) {
    log.debug("remove user signout key");
    authService.removeUserSignoutKey(userId);
  }
}
