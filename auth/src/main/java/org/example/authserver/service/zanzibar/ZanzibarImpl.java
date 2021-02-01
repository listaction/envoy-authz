package org.example.authserver.service.zanzibar;

import authserver.acl.Acl;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class ZanzibarImpl implements Zanzibar {

    private final AclRepository repository;
    private final AclRelationConfigService relationConfigService;

    public ZanzibarImpl(AclRepository repository, AclRelationConfigService relationConfigService) {
        this.repository = repository;
        this.relationConfigService = relationConfigService;
    }

    @Override
    public boolean check(String namespace, String object, String relation, String principal) {
        String tag = String.format("%s:%s#%s", namespace, object, relation);
        log.info("expected tag: {}", tag);
        Set<String> relations = getRelations(namespace, object, principal);
        log.info("relations available: {}", relations);
        return relations.contains(tag);
    }

    @Override
    public Set<String> getRelations(String namespace, String object, String principal) {
        Set<ExpandedAcl> relations = expand(namespace, object, principal);
        Set<String> l = lookup(relations, namespace, object, principal);
        return l;
    }

    private Set<String> lookup(Set<ExpandedAcl> relations, String namespace, String object, String principal) {
        Set<String> result = new HashSet<>();
        Set<ExpandedAcl> filtered = filter(relations, namespace, object);
        for (ExpandedAcl t : filtered){
            String user = t.getUser();
            if (principal.equals(user)){
                result.add(generateTag(t.getNamespace(), t.getObject(), t.getRelation()));
            } else if (Strings.isEmpty(user)){
                Set<String> nested = lookup(relations, t.getUsersetNamespace(), t.getUsersetObject(), principal);
                if (nested.size() > 0) {
                    String rightPart = generateTag(t.getUsersetNamespace(), t.getUsersetObject(), t.getUsersetRelation());
                    if (nested.contains(rightPart)) {
                        result.add(generateTag(t.getNamespace(), t.getObject(), t.getRelation()));
                    }
                    result.addAll(nested);
                }
            }
        }
        return result;
    }

    private Set<ExpandedAcl> filter(Set<ExpandedAcl> relations, String namespace, String object) {
        Set<ExpandedAcl> result = new HashSet<>();
        for (ExpandedAcl r : relations) {
            if (namespace.equals(r.getNamespace()) && object.equals(r.getObject())) {
                result.add(r);
            }
        }

        return result;
    }

    private Set<ExpandedAcl> expand(String namespace, String object, String principal) {
        Set<ExpandedAcl> relations = new HashSet<>();
        Set<Acl> acls = repository.findAllByNamespaceAndObjectAndPrincipal(namespace, object, principal);
        for (Acl acl : acls) {
            Set<String> nested = relationConfigService.nestedRelations(acl.getNamespace(), acl.getObject(), acl.getRelation());
            if (acl.hasUserset()) {
                Set<String> roots = relationConfigService.rootRelations(acl.getUsersetNamespace(), acl.getUsersetObject(), acl.getUsersetRelation());
                for (String rel : nested) {
                    for (String rootRel : roots) {
                        ExpandedAcl expandedAcl = ExpandedAcl.builder()
                                .namespace(acl.getNamespace())
                                .object(acl.getObject())
                                .relation(rel)
                                .usersetNamespace(acl.getUsersetNamespace())
                                .usersetObject(acl.getUsersetObject())
                                .usersetRelation(rootRel)
                                .build();
                        relations.add(expandedAcl);
                        relations.addAll(expand(acl.getUsersetNamespace(), acl.getUsersetObject(), principal));
                    }
                }
            } else {
                for (String rel : nested) {
                    ExpandedAcl expandedAcl = ExpandedAcl.builder()
                            .namespace(acl.getNamespace())
                            .object(acl.getObject())
                            .relation(rel)
                            .user(acl.getUser())
                            .build();
                    relations.add(expandedAcl);
                }
            }
        }
        return relations;
    }

    @Override
    public void addRule(String aclExpr) {
        Acl acl = Acl.create(aclExpr);
        if (acl == null) throw new RuntimeException("Bad acl expression: " + aclExpr);
        repository.save(acl);
    }

    @Override
    public void removeRule(String aclExpr) {
        Acl acl = Acl.create(aclExpr);
        if (acl == null) throw new RuntimeException("Bad acl expression: " + aclExpr);
        repository.delete(acl);
    }

    private String generateTag(String namespace, String object, String relation) {
        return String.format("%s:%s#%s", namespace, object, relation);
    }


    @Getter
    @Builder
    public static class ExpandedAcl {
        private String namespace;
        private String object;
        private String relation;
        private String user;

        private String usersetNamespace;
        private String usersetObject;
        private String usersetRelation;

        @Override
        public String toString() {
            if (!Strings.isEmpty(user)){
                return String.format("%s:%s#%s@%s", namespace, object, relation, user);
            }
            return String.format("%s:%s#%s@%s:%s#%s", namespace, object, relation, usersetNamespace, usersetObject, usersetRelation);
        }
    }

}
