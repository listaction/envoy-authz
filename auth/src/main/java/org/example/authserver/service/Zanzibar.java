package org.example.authserver.service;

import java.util.Set;

public interface Zanzibar {

    boolean check(String namespace, String object, String relation, String principal);
    Set<String> getRelations(String namespace, String object, String principal);
    void addRule(String aclExpr);
    void removeRule(String aclExpr);

}
