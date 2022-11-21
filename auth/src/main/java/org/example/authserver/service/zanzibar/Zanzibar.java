package org.example.authserver.service.zanzibar;

import java.util.Set;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.entity.LocalCache;

public interface Zanzibar {

  CheckResult check(
      String namespace, String object, String relation, String principal, LocalCache requestCache);

  Set<String> getRelations(
      String namespace, String object, String principal, LocalCache requestCache);

  void addRule(String aclExpr);

  void removeRule(String aclExpr);
}
