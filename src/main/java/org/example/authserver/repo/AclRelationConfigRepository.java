package org.example.authserver.repo;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.domain.AclRelationConfig;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class AclRelationConfigRepository {

    private static final Map<String, AclRelationConfig> data = new ConcurrentHashMap<>();

    @PostConstruct
    public void init(){
        log.info("test");
    }

    public Set<AclRelationConfig> findAll(){
        return new HashSet<>(data.values());
    }


    public AclRelationConfig findOneByNamespace(String namespace){
        return data.get(namespace);
    }

    public void save(AclRelationConfig config) {
        data.put(config.getNamespace(), config);
    }


}
