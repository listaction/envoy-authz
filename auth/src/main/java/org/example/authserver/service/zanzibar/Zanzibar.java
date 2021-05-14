package org.example.authserver.service.zanzibar;

import org.example.authserver.entity.CheckResult;

import java.util.Set;

public interface Zanzibar {

    CheckResult check(String namespace, String object, String relation, String principal);
    Set<String> getRelations(String namespace, String object, String principal);
    void addRule(String aclExpr);
    void removeRule(String aclExpr);

}
