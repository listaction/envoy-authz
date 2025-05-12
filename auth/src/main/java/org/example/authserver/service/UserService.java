package org.example.authserver.service;

import authserver.acl.Acl;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final AclService aclService;

    public UserService(AclService aclService) {
        this.aclService = aclService;
    }

    public Set<Acl> assignRolesToUser(String userId, @NotEmpty Set<String> roles) {
        log.info("Assigning roles to user: {}, roles: {}", userId, roles);
        Set<Acl> acls = new HashSet<>();
        // _rbac:_users#role -> userId
        for (String role : roles) {
            Acl acl = new Acl();
            acl.setId(UUID.randomUUID());
            acl.setNamespace("_rbac");
            acl.setObject("_users");
            acl.setRelation(role);
            acl.setUser(userId);

            aclService.save(acl);
            acls.add(acl);
        }
        return acls;
    }

    public Set<Acl> deleteUser(String userId) {
        Set<Acl> aclList = aclService.findAllByPrincipal(userId);
        for (Acl acl : aclList) {
            aclService.delete(acl);
        }
        return aclList;
    }
}
