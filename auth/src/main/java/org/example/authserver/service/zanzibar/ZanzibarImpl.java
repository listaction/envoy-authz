package org.example.authserver.service.zanzibar;

import authserver.acl.Acl;
import authserver.acl.AclRelation;
import io.micrometer.core.annotation.Timed;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.service.model.RequestCache;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ZanzibarImpl implements Zanzibar {

    private final AclRepository repository;
    private final AclRelationConfigService relationConfigService;

    public ZanzibarImpl(AclRepository repository, AclRelationConfigService relationConfigService) {
        this.repository = repository;
        this.relationConfigService = relationConfigService;
    }

    @Timed(value = "checkAcl", percentiles = {0.99, 0.95, 0.75})
    @Override
    public CheckResult check(String namespace, String object, String relation, String principal, RequestCache requestCache) {
        String tag = String.format("%s:%s#%s", namespace, object, relation);
        log.trace("expected tag: {}", tag);
        Set<String> relations = getRelations(namespace, object, principal, requestCache);

        log.trace("relations available: {}", relations);
        return CheckResult.builder()
                .result(relations.contains(tag))
                .tags(relations)
                .build();
    }

    @Override
    @Timed(value = "relation.zanzibar", percentiles = {0.99, 0.95, 0.75})
    public Set<String> getRelations(String namespace, String object, String principal, RequestCache requestCache) {
        Set<ExpandedAcl> relations = expandMultiple(Set.of(Tuples.of(namespace, object)), principal, requestCache);
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
                log.trace("relation {}#{} is excluded [exclusion]", l.getT1(), l.getT2());
            } else if (intersections.size() > 0 && Collections.disjoint(intersections, lookups)) {
                log.trace("relation {}#{} is excluded [interception]", l.getT1(), l.getT2());
            } else {
                result.add(generateTag(l.getT1(), l.getT2()));
            }

        }

        return result;
    }

    @Timed(value = "lookup", percentiles = {0.99, 0.95, 0.75})
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

    @Timed(value = "expandMultiple", percentiles = {0.99, 0.95, 0.75})
    private Set<ExpandedAcl> expandMultiple(Set<Tuple2<String, String>> namespaceObjects, String principal, RequestCache requestCache){
        log.trace("calling expandMultiple [cache: {}] =>  {}", requestCache.getCache().size(), namespaceObjects);
        if (namespaceObjects.size() == 0){
            return new HashSet<>();
        }

        List<String> nsObjects = namespaceObjects.stream()
                .map(tuple->String.format("%s:%s", tuple.getT1(), tuple.getT2()))
                .collect(Collectors.toList());

        Set<Acl> acls = new HashSet<>();
        if (requestCache.getPrincipalAclCache().containsKey(principal)) {
            acls.addAll(requestCache.getPrincipalAclCache().get(principal));
        } else {
            Set<Acl> principalAcls = repository.findAllByPrincipal(principal);
            acls.addAll(principalAcls);
            requestCache.getPrincipalAclCache().put(principal, principalAcls);
        }
        acls.addAll(repository.findAllByNsObjectIn(nsObjects));

        Set<ExpandedAcl> result = new HashSet<>(acls.size());
        for (Acl acl : acls){
            for (Tuple2<String, String> tuple : namespaceObjects){
                String ns = String.format("%s:%s", tuple.getT1(), tuple.getT2());
                if (acl.getNsObject().equalsIgnoreCase(ns)){
                    Set<ExpandedAcl> tmp = expand(tuple.getT1(), tuple.getT2(), principal, acls, requestCache);
                    result.addAll(tmp);
                }
            }
        }
        return result;
    }

    @Timed(value = "expandNoDbQuery", percentiles = {0.99, 0.95, 0.75})
    private Set<ExpandedAcl> expand(String namespace, String object, String principal, Set<Acl> acls, RequestCache requestCache) {
        Map<Tuple2<String, String>, Set<ExpandedAcl>> cache = requestCache.getCache();
        Map<String, Set<Acl>> principalAclCache = requestCache.getPrincipalAclCache();

        if (cache.containsKey(Tuples.of(namespace, object))){
            Set<ExpandedAcl> setFromCache = cache.get(Tuples.of(namespace, object));
            for (Acl acl : principalAclCache.getOrDefault(principal, new HashSet<>())) {
                ExpandedAcl expandedAcl = ExpandedAcl.builder()
                        .namespace(acl.getNamespace())
                        .object(acl.getObject())
                        .relation(acl.getRelation())
                        .user(acl.getUser())
                        .build();
                setFromCache.add(expandedAcl);
            }
            return setFromCache;
        }
        Set<ExpandedAcl> relations = new HashSet<>();
        for (Acl acl : acls) {
            Set<String> nested = relationConfigService.nestedRelations(acl.getNamespace(), acl.getObject(), acl.getRelation());
            if (acl.hasUserset()) {
                Set<Tuple2<String, String>> aclsToExpand = new HashSet<>();
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
                        if (cache.containsKey(Tuples.of(acl.getUsersetNamespace(), acl.getUsersetObject()))){
                            Set<ExpandedAcl> dataFromCache = cache.get(Tuples.of(acl.getUsersetNamespace(), acl.getUsersetObject()));
                            relations.addAll(dataFromCache);
                        } else {
                            aclsToExpand.add(Tuples.of(acl.getUsersetNamespace(), acl.getUsersetObject()));
                        }
                    }
                }
                relations.addAll(expandMultiple(aclsToExpand, principal, requestCache));
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

        cache.put(Tuples.of(namespace, object), relations);
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExpandedAcl)) return false;
            ExpandedAcl that = (ExpandedAcl) o;
            return Objects.equals(namespace, that.namespace) && Objects.equals(object, that.object) && Objects.equals(relation, that.relation) && Objects.equals(user, that.user) && Objects.equals(usersetNamespace, that.usersetNamespace) && Objects.equals(usersetObject, that.usersetObject) && Objects.equals(usersetRelation, that.usersetRelation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, object, relation, user, usersetNamespace, usersetObject, usersetRelation);
        }

        @Override
        public String toString() {
            if (!Strings.isEmpty(user)){
                return String.format("%s:%s#%s@%s", namespace, object, relation, user);
            }
            return String.format("%s:%s#%s@%s:%s#%s", namespace, object, relation, usersetNamespace, usersetObject, usersetRelation);
        }
    }

}
