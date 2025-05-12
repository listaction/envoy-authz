package org.example.authserver.repo;

import org.example.authserver.entity.RoleEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RoleRepository extends CrudRepository<RoleEntity, String> {

  List<RoleEntity> findAll();

}
