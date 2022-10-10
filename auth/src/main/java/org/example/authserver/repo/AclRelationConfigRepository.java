package org.example.authserver.repo;

import java.util.List;
import org.example.authserver.entity.AclRelationConfigEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AclRelationConfigRepository
    extends CrudRepository<AclRelationConfigEntity, String> {

  List<AclRelationConfigEntity> findAll();
}
