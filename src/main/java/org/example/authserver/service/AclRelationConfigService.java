package org.example.authserver.service;

import org.example.authserver.domain.AclRelationConfig;
import org.example.authserver.domain.AclRelationParent;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@Service
public class AclRelationConfigService {

    private final AclRelationConfigRepository repository;

    public AclRelationConfigService(AclRelationConfigRepository repository) {
        this.repository = repository;
    }

    public Set<String> nestedRelations(String namespace, String objectId, String relation) {
        AclRelationConfig config = repository.findOneByNamespace(namespace);
        if (config == null) return new HashSet<>();
        if (config.getRelations() == null) return new HashSet<>();

        return config.getRelations().stream()
                .filter(f->objectId.equals(f.getObjectId()))
                .map(m-> {
                    Set<Tuple2<String, String>> relations = expandRelations(m.getRelation(), m.getParents());
                    return relations;
                })
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(m->m.getT2(), Collectors.mapping(m->m.getT1(), toSet())))
                .getOrDefault(relation, new HashSet<>());
    }

    private Set<Tuple2<String, String>> expandRelations(String child, Set<AclRelationParent> relations) {
        Set<Tuple2<String, String>> result = new HashSet<>();
        if (relations == null) return result;
        for (AclRelationParent p : relations) {
            result.add(Tuples.of(child, child)); // self
            result.add(Tuples.of(child, p.getRelation()));
            if (p.getParents() != null) {
                result.addAll(expandRelations(child, p.getParents()));
            }
        }

        return result;
    }

}
