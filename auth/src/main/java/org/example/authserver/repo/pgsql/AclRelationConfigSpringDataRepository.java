package org.example.authserver.repo.pgsql;

import org.example.authserver.entity.AclRelationConfigEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AclRelationConfigSpringDataRepository extends CrudRepository<AclRelationConfigEntity, String> {

    List<AclRelationConfigEntity> findAll();

}
