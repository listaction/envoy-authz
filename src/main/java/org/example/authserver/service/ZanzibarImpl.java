package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.domain.Acl;
import org.example.authserver.repo.AclRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ZanzibarImpl implements Zanzibar {

    private final AclRepository repository;
    private final AclRelationConfigService relationConfigService;

    private final Pattern pattern = Pattern.compile("(\\S+):(\\S+)");

    public ZanzibarImpl(AclRepository repository, AclRelationConfigService relationConfigService) {
        this.repository = repository;
        this.relationConfigService = relationConfigService;
    }

    @Override
    public boolean check(String aclExpr, String principal, String requiredRole) {
        String namespace;
        String objectId;

        Matcher m = pattern.matcher(aclExpr);
        if (m.find()){
            namespace = m.group(1);
            objectId = m.group(2);
        } else {
            throw new RuntimeException();
        }

        log.info("Parsed acl expr ->  namespace: {}, objectId: {}", namespace, objectId);

        Set<Acl> acls = repository.findAll().stream()
                .filter(f->f.getObject().equals(String.format("%s:%s", namespace, objectId)))
                .filter(f->f.getUserset().equals(principal)) //todo: expand userset
                .collect(Collectors.toSet());

        for (Acl acl : acls){
            Set<String> roles = relationConfigService.nestedRelations(namespace, objectId, acl.getRelation());
            if (roles.contains(requiredRole)) {
                return true;
            }
        }

        return false;
    }


}
