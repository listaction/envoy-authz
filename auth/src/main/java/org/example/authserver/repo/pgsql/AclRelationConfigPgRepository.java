package org.example.authserver.repo.pgsql;

import authserver.acl.AclRelationConfig;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.Utils;
import org.example.authserver.entity.AclRelationConfigEntity;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConditionalOnProperty(
        value="app.database",
        havingValue = "POSTGRES"
)
public class AclRelationConfigPgRepository implements AclRelationConfigRepository {

    private final AclRelationConfigSpringDataRepository repository;

    public AclRelationConfigPgRepository(AclRelationConfigSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<AclRelationConfig> findAll() {
        return repository.findAll().stream()
                .map(AclRelationConfigEntity::toAclRelationConfig)
                .collect(Collectors.toSet());
    }

    @Override
    public AclRelationConfig findOneById(String id) {
        return repository.findById(id)
                .map(AclRelationConfigEntity::toAclRelationConfig)
                .orElse(null);
    }

    @Override
    public void save(AclRelationConfig config) {
        AclRelationConfigEntity entity = AclRelationConfigEntity.builder()
                .id(config.getId().toString())
                .namespace(config.getNamespace())
                .config(config)
                .build();

        repository.save(entity);
    }

    @Override
    public void delete(AclRelationConfig config) {
        repository.deleteById(config.getId().toString());
    }

}
