package org.example.authserver.service;

public interface Zanzibar {

    boolean check(String aclExpr, String principal, String requiredRole);

}
