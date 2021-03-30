package org.example.authserver.repo.pgsql;

import org.example.authserver.entity.AclEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AclSpringDataRepository extends CrudRepository<AclEntity, String> {

    List<AclEntity> findAll();
    Set<AclEntity> findAllByNsobjectAndUser(String nsobject, String user);

}
