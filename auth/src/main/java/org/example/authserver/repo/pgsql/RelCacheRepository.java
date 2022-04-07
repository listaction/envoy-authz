package org.example.authserver.repo.pgsql;

import org.example.authserver.entity.RelCache;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelCacheRepository extends CrudRepository<RelCache, String> {

    List<RelCache> findAllByUsrAndRevAndNsobjectAndPath(String principal, Long rev, String nsObject, String path);

    List<RelCache> findAllByUsrAndRevLessThan(String principal, Long rev);
}
