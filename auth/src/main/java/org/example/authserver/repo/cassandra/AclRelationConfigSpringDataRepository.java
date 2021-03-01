package org.example.authserver.repo.cassandra;

import org.example.authserver.entity.AclRelationConfigEntity;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AclRelationConfigSpringDataRepository extends CassandraRepository<AclRelationConfigEntity, UUID> {
}
