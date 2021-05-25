package org.example.authserver.repo.pgsql;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.AclEntity;
import org.example.authserver.repo.AclRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
@ConditionalOnProperty(
        value="app.database",
        havingValue = "POSTGRES"
)
public class AclPgRepository implements AclRepository {

    private final AclSpringDataRepository repository;

    public AclPgRepository(AclSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<Acl> findAll() {
        return repository.findAll().stream()
                .map(AclEntity::toAcl)
                .collect(Collectors.toSet());
    }

    @Override
    public Acl findOneById(String id) {
        return repository.findById(id)
                .map(AclEntity::toAcl)
                .orElse(null);
    }

    @Override
    public Set<Acl> findAllByNamespaceAndObjectAndUser(String namespace, String object, String user) {
        Set<AclEntity> usersetAcls = repository.findAllByNsobjectAndUser(String.format("%s:%s", namespace, object), "*");
        Set<AclEntity> userAcls = repository.findAllByNsobjectAndUser(String.format("%s:%s", namespace, object), user);
        return Stream.concat(usersetAcls.stream(), userAcls.stream())
                .map(AclEntity::toAcl)
                .collect(Collectors.toSet());
    }


    @Override
    public void save(Acl acl) {
        AclEntity entity = AclEntity.builder()
                .id(acl.getId().toString())
                .nsobject(String.format("%s:%s", acl.getNamespace(), acl.getObject()))
                .namespace(acl.getNamespace())
                .object(acl.getObject())
                .relation(acl.getRelation())
                .user((acl.hasUserset()) ? "*" : acl.getUser())
                .usersetNamespace(acl.getUsersetNamespace())
                .usersetObject(acl.getUsersetObject())
                .usersetRelation(acl.getUsersetRelation())
                .build();

        repository.save(entity);
    }

    @Override
    public void delete(Acl acl) {
        repository.deleteById(acl.getId().toString());
    }

}