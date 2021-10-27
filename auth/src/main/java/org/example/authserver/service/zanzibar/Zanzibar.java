package org.example.authserver.service.zanzibar;

import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.model.RequestCache;

import java.util.Set;

public interface Zanzibar {

    CheckResult check(String namespace, String object, String relation, String principal, RequestCache requestCache);
    Set<String> getRelations(String namespace, String object, String principal, RequestCache requestCache);
    void addRule(String aclExpr);
    void removeRule(String aclExpr);

}
