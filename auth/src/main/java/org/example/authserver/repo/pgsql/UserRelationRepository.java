package org.example.authserver.repo.pgsql;

import org.example.authserver.entity.UserRelationEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRelationRepository extends CrudRepository<UserRelationEntity, String> {
}
