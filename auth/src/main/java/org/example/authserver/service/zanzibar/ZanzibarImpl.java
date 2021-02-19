package org.example.authserver.service.zanzibar;

import authserver.acl.Acl;
import authserver.acl.AclRelation;
import authserver.acl.AclRelationConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.Collections;
import java.util.HashSet;
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
        Set<Tuple2<String, String>> lookups = lookup(relations, namespace, object, principal);

        Set<String> result = new HashSet<>();
        for (Tuple2<String, String> l : lookups){
            Set<Tuple2<String, String>> exclusions = new HashSet<>();
            Set<Tuple2<String, String>> intersections = new HashSet<>();

            AclRelation relation = relationConfigService.getConfigRelation(l.getT1(), l.getT2());
            if (relation == null) {
                result.add(generateTag(l.getT1(), l.getT2()));
                continue; // some relations are not described with configs
            }

            for (String exclusion : relation.getExclusions()){
                exclusions.add(Tuples.of(l.getT1(), exclusion));
            }

            for (String intersection : relation.getIntersections()){
                intersections.add(Tuples.of(l.getT1(), intersection));
            }

            if (!Collections.disjoint(exclusions, lookups)){
                log.info("relation {}#{} is excluded [exclusion]", l.getT1(), l.getT2());
            } else if (intersections.size() > 0 && Collections.disjoint(intersections, lookups)) {
                log.info("relation {}#{} is excluded [interception]", l.getT1(), l.getT2());
            } else {
                result.add(generateTag(l.getT1(), l.getT2()));
            }

        }

        return result;
    }

    private Set<Tuple2<String, String>> lookup(Set<ExpandedAcl> relations, String namespace, String object, String principal) {
        Set<Tuple2<String, String>> result = new HashSet<>(); // Tuples of {namespace:object, relation}
        Set<ExpandedAcl> filtered = filter(relations, namespace, object);
        for (ExpandedAcl t : filtered){
            String user = t.getUser();
            if (principal.equals(user)){
                result.add(Tuples.of(String.format("%s:%s", t.getNamespace(), t.getObject()), t.getRelation()));
            } else if (Strings.isEmpty(user)){
                Set<Tuple2<String, String>> nested = lookup(relations, t.getUsersetNamespace(), t.getUsersetObject(), principal);
                if (nested.size() > 0) {
                    Tuple2<String, String> rightPart = Tuples.of(String.format("%s:%s", t.getUsersetNamespace(), t.getUsersetObject()), t.getUsersetRelation());
                    if (nested.contains(rightPart)) {
                        result.add(Tuples.of(String.format("%s:%s", t.getNamespace(), t.getObject()), t.getRelation()));
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

    private String generateTag(String namespaceObject, String relation) {
        return String.format("%s#%s", namespaceObject, relation);
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
