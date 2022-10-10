package org.example.authserver.repo;

import java.util.List;
import org.example.authserver.entity.MappingEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MappingRepository extends CrudRepository<MappingEntity, String> {

  List<MappingEntity> findAll();
}
