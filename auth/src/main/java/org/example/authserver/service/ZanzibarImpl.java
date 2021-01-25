package org.example.authserver.service;

import lombok.extern.slf4j.Slf4j;
import authserver.acl.Acl;
import org.example.authserver.repo.AclRepository;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

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
        Set<Acl> acls = filterAcls(principal, repository.findAllByNamespaceAndObject(namespace, object));
        for (Acl acl : new HashSet<>(acls)){ //todo: redesign
            if (acl.hasUserset()){
                acls.addAll(
                        repository.findAllByNamespaceAndObject(acl.getUsersetNamespace(), acl.getUsersetObject()).stream()
                        .filter(f->!f.hasUserset() && f.getUser().equals(principal))
                        .collect(Collectors.toList())
                );
            }
        }
        log.info("acls[{}]: {}", acls.size(), acls);
        Set<Acl> expandedAcls = new HashSet<>();
        for (Acl acl : acls){
            expandedAcls.add(acl);
            expandedAcls.addAll(unionRelations(acl));
            if (!acl.hasUserset()) {
                continue;
            }
            Set<Acl> tmp = expandAcls(acl, principal);
            expandedAcls.addAll(tmp);
            for (Acl tmpAcl : tmp) {
                expandedAcls.addAll(unionRelations(tmpAcl));
            }
        }

        Map<String, Set<Acl>> grouped = expandedAcls.stream()
                .map(acl -> {
                    String tag = generateTag(acl.getNamespace(), acl.getObject(), acl.getRelation());
                    return Tuples.of(tag, acl);
                })
                .collect(Collectors.groupingBy(Tuple2::getT1, Collectors.mapping(Tuple2::getT2, toSet())));

        Iterator<Map.Entry<String, Set<Acl>>> it = grouped.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<String, Set<Acl>> entry = it.next();
            Set<Acl> aclSet = entry.getValue();
            if (aclSet == null){
                it.remove();
                continue;
            }

            aclSet.removeIf(acl -> !filterAclGroup(acl, grouped));
            if (aclSet.size() == 0) it.remove();
        }

        return grouped.keySet();
    }

    private Set<Acl> unionRelations(Acl acl) {
        Set<Acl> result = new HashSet<>();
        if (acl.hasUserset()){
            Set<String> nested = relationConfigService.nestedRelations(String.format("%s:%s", acl.getUsersetNamespace(), acl.getUsersetObject()), acl.getUsersetRelation());
            for (String rel : nested){
                if (acl.getUsersetRelation().equals(rel)) continue;
                Acl clone = acl.clone();
                clone.setRelation(rel);
                result.add(clone);
            }
        } else {
            Set<String> nested = relationConfigService.nestedRelations(String.format("%s:%s", acl.getNamespace(), acl.getObject()), acl.getRelation());
            for (String rel : nested){
                if (acl.getRelation().equals(rel)) continue;
                Acl clone = acl.clone();
                clone.setRelation(rel);
                result.add(clone);
            }
        }

        return result;
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

    private boolean filterAclGroup(Acl acl, Map<String, Set<Acl>> grouped) {
        if (!acl.hasUserset()) return true;
        String tag = generateTag(acl.getUsersetNamespace(), acl.getUsersetObject(), acl.getUsersetRelation());
        return grouped.containsKey(tag);
    }

    private Set<Acl> filterAcls(String principal, Set<Acl> acls) {
        acls.removeIf(acl -> !acl.hasUserset() && !acl.getUser().equals(principal));
        return acls;
    }

    private Set<Acl> filterAcls1(String principal, Set<Acl> acls) {
        Set<Acl> tmp = new HashSet<>(acls);
        tmp.removeIf(acl -> !acl.hasUserset() && !acl.getUser().equals(principal));

        Set<String> allowedRelations = new HashSet<>();
        for (Acl acl : tmp){
            Set<String> nested = relationConfigService.nestedRelations(String.format("%s:%s", acl.getNamespace(), acl.getObject()), acl.getRelation());
            allowedRelations.addAll(nested);
        }

        Iterator<Acl> it = acls.iterator();
        while (it.hasNext()){
            Acl acl = it.next();
            if (acl.hasUserset()) continue;


            if (!allowedRelations.contains(acl.getRelation())) {
                it.remove();
            }
        }

        return acls;
    }

    private Set<Acl> expandAcls(Acl acl, String principal) {
        if (acl.hasUserset()){
            return filterAcls1(principal, repository.findAllByNamespaceAndObject(acl.getUsersetNamespace(), acl.getUsersetObject()));
        } else {
            return (acl.getUser().equals(principal)) ? Set.of(acl) : new HashSet<>();
        }
    }

}
