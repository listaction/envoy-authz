package org.example.authserver.repo;

import org.example.authserver.entity.MappingEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MappingRepository extends CrudRepository<MappingEntity, String> {

    List<MappingEntity> findAll();

}
