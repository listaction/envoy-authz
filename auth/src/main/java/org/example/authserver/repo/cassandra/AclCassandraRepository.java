package org.example.authserver.repo.cassandra;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.AclEntity;
import org.example.authserver.repo.AclRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConditionalOnProperty(
        value="app.database",
        havingValue = "CASSANDRA"
)
public class AclCassandraRepository implements AclRepository {

    private final AclSpringDataRepository cassandraRepository;

    public AclCassandraRepository(AclSpringDataRepository cassandraRepository) {
        this.cassandraRepository = cassandraRepository;
    }

    @Override
    public Set<Acl> findAll() {
        return cassandraRepository.findAll().stream()
                .map(AclEntity::toAcl)
                .collect(Collectors.toSet());
    }

    @Override
    public Acl findOneById(String id) {
        return cassandraRepository.findById(UUID.fromString(id))
                .map(AclEntity::toAcl)
                .orElse(null);
    }

    @Override
    public Set<Acl> findAllByNamespaceAndObjectAndUser(String namespace, String object, String user) {
        return cassandraRepository.findAllByNamespaceAndObject(namespace, object).stream()
                .map(AclEntity::toAcl)
                .filter(f->(f.hasUserset() || (!f.hasUserset() && user.equals(f.getUser()))))
                .collect(Collectors.toSet());
    }


    @Override
    public void save(Acl acl) {
        AclEntity entity = AclEntity.builder()
                .namespace(acl.getNamespace())
                .object(acl.getObject())
                .relation(acl.getRelation())
                .user(acl.getUser())
                .usersetNamespace(acl.getUsersetNamespace())
                .usersetObject(acl.getUsersetObject())
                .usersetRelation(acl.getUsersetRelation())
                .build();

        cassandraRepository.save(entity);
    }

    @Override
    public void delete(Acl acl) {
        cassandraRepository.deleteById(acl.getId());
    }

}
