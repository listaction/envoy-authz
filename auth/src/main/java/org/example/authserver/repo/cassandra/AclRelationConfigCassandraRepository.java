package org.example.authserver.repo.cassandra;

import authserver.acl.AclRelationConfig;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.Utils;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.AclRelationConfigEntity;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.example.authserver.repo.cassandra.AclRelationConfigSpringDataRepository;
import org.example.authserver.repo.redis.AclRelationConfigRedisRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConditionalOnProperty(
        value="app.database",
        havingValue = "CASSANDRA"
)
public class AclRelationConfigCassandraRepository implements AclRelationConfigRepository {

    private final AclRelationConfigSpringDataRepository cassandraRepository;

    public AclRelationConfigCassandraRepository(AclRelationConfigSpringDataRepository cassandraRepository) {
        this.cassandraRepository = cassandraRepository;
    }

    @Override
    public Set<AclRelationConfig> findAll() {
        return cassandraRepository.findAll().stream()
                .map(AclRelationConfigEntity::toAclRelationConfig)
                .collect(Collectors.toSet());
    }

    @Override
    public AclRelationConfig findOneById(String id) {
        return cassandraRepository.findById(UUID.fromString(id))
                .map(AclRelationConfigEntity::toAclRelationConfig)
                .orElse(null);
    }

    @Override
    public void save(AclRelationConfig config) {
        AclRelationConfigEntity entity = AclRelationConfigEntity.builder()
                .id(config.getId())
                .namespace(config.getNamespace())
                .json(Utils.configToJson(config))
                .build();

        cassandraRepository.save(entity);
    }

    @Override
    public void delete(AclRelationConfig config) {
        cassandraRepository.deleteById(config.getId());
    }

}
