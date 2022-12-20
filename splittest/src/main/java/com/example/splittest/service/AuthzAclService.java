package com.example.splittest.service;

import authserver.acl.Acl;
import com.example.splittest.config.AuthzClient;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthzAclService {

  private final Executor executor = Executors.newScheduledThreadPool(20);
  private final AuthzClient authzClient;

  public AuthzAclService(AuthzClient authzClient) {
    this.authzClient = authzClient;
  }

  public void createAcl(Acl acl) {
    executor.execute(
        () -> {
          try {
            authzClient.createAcl("", acl);
          } catch (Exception e) {
            log.warn("Can't create acl: {}", acl, e);
          }
        });
  }

  public void deleteAcl(Acl acl) {
    executor.execute(
        () -> {
          try {
            authzClient.deleteAcl("", acl);
          } catch (Exception e) {
            log.warn("Can't delete acl: {}", acl, e);
          }
        });
  }
}
