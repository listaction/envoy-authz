package org.example.authserver.entity;

import authserver.acl.Acl;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.authserver.service.zanzibar.ZanzibarImpl;
import reactor.util.function.Tuple2;

@Getter
@NoArgsConstructor
public class LocalCache {

  private final Map<Tuple2<String, String>, Set<ZanzibarImpl.ExpandedAcl>> cache = new HashMap<>();
  private final Map<String, Set<Acl>> principalAclCache = new HashMap<>();
  private final Map<String, Set<String>> principalHighCardinalityCache = new HashMap<>();

  public Acl getMaxAcl(String user) {
    Set<Acl> acls = principalAclCache.get(user);
    if (acls == null || acls.isEmpty()) return null;
    return Collections.max(acls, Comparator.comparingLong(Acl::getUpdated));
  }

  public long getMaxAclUpdated(String user) {
    Acl maxAcl = getMaxAcl(user);
    return maxAcl != null ? maxAcl.getUpdated() : 0;
  }
}
