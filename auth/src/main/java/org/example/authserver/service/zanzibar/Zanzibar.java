package org.example.authserver.service.zanzibar;

import authserver.acl.Acl;
import org.example.authserver.entity.CheckResult;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.Set;

public interface Zanzibar {

    CheckResult check(String namespace, String object, String relation, String principal, Map<Tuple2<String, String>, Set<ZanzibarImpl.ExpandedAcl>> cache, Map<String, Set<Acl>> principalAclCache);
    Set<String> getRelations(String namespace, String object, String principal, Map<Tuple2<String, String>, Set<ZanzibarImpl.ExpandedAcl>> cache, Map<String, Set<Acl>> principalAclCache);
    void addRule(String aclExpr);
    void removeRule(String aclExpr);

}
