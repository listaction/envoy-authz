package org.example.authserver.service;

import io.micrometer.core.annotation.Timed;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.LocalCache;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RelationsService {

  private final Zanzibar zanzibar;
  private final AclService aclService;
  private final MeterService meterService;

  public RelationsService(Zanzibar zanzibar, AclService aclService, MeterService meterService) {
    this.zanzibar = zanzibar;
    this.aclService = aclService;
    this.meterService = meterService;
  }

  public Long getAclMaxUpdate(String principal) {
    return aclService.findMaxAclUpdatedByPrincipal(principal);
  }

  @Timed(
      value = "relation.get",
      percentiles = {0.99, 0.95, 0.75})
  public Set<String> getRelations(
      String namespace, String object, String principal, LocalCache localCache) {
    meterService.countHitsZanzibar();
    return zanzibar.getRelations(namespace, object, principal, new LocalCache());
  }
}
