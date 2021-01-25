package org.example.authserver.service;

import authserver.acl.AclRelation;
import authserver.acl.AclRelationConfig;
import authserver.acl.AclRelationParent;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AclRelationConfigService {

    private final AclRelationConfigRepository repository;

    public AclRelationConfigService(AclRelationConfigRepository repository) {
        this.repository = repository;
    }

    public Set<String> nestedRelations(String namespace, String relation) {
        AclRelationConfig config = repository.findOneByNamespace(namespace);
        if (config == null) return new HashSet<>();
        if (config.getRelations() == null) return new HashSet<>();

        Set<AclRelation> relations = expandRelations(relation, config.getRelations());
        Set<String> result = relations.stream()
                .map(AclRelation::getRelation)
                .collect(Collectors.toSet());

        result.add(relation);
        return result;
    }

    private Set<AclRelation> expandRelations(String parent, Set<AclRelation> relations) {
        Set<AclRelation> result = new HashSet<>();
        for (AclRelation r : relations){
            if (r.getParents() != null){
                for (AclRelationParent p : r.getParents()){
                    if (p.getRelation().equals(parent)){
                        result.add(r);
                        result.addAll(expandRelations(r.getRelation(), relations));
                    }
                }
            }
        }
        return result;
    }

}
