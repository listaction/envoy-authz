package org.example.authserver.repo.cassandra;

import org.example.authserver.entity.AclEntity;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface AclSpringDataRepository extends CassandraRepository<AclEntity, UUID> {
    @AllowFiltering
    Set<AclEntity> findAllByNamespaceAndObject(String namespace, String object);
}
