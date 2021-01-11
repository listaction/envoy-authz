package org.example.authserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.domain.Acl;
import org.example.authserver.domain.AclRelation;
import org.example.authserver.domain.AclRelationConfig;
import org.example.authserver.domain.AclRelationParent;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.example.authserver.repo.AclRepository;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


import javax.annotation.PostConstruct;
import java.util.Set;

import static org.example.authserver.config.Constants.ACL_REDIS_KEY;

@Slf4j
@Service
public class ExampleDataset {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final JedisPool jedis;
    private final AclRepository aclRepository;
    private final AclRelationConfigRepository relationConfigRepository;

    public ExampleDataset(JedisPool jedis, AclRepository aclRepository, AclRelationConfigRepository relationConfigRepository) {
        this.jedis = jedis;
        this.aclRepository = aclRepository;
        this.relationConfigRepository = relationConfigRepository;
    }
    
    public void init() {
        log.info("Initialize example dataset");
        Jedis conn = jedis.getResource();
        conn.del(ACL_REDIS_KEY);

        Acl acl1 = new Acl();
        acl1.setObject("doc:readme");
        acl1.setRelation("viewer");
        acl1.setUserset("user1_viewer");

        Acl acl2 = new Acl();
        acl2.setObject("doc:readme");
        acl2.setRelation("editor");
        acl2.setUserset("user2_editor");

        Acl acl3 = new Acl();
        acl3.setObject("doc:readme");
        acl3.setRelation("owner");
        acl3.setUserset("user3_owner");

        aclRepository.save(acl1);
        aclRepository.save(acl2);
        aclRepository.save(acl3);

        conn.publish(ACL_REDIS_KEY, "update");

        try {
            initRelationConfigs();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void initRelationConfigs() throws JsonProcessingException {
        AclRelationConfig relationConfig = new AclRelationConfig();
        relationConfig.setNamespace("doc");
        relationConfig.setRelations(Set.of(
                AclRelation.builder()
                        .objectId("readme")
                        .relation("owner")
                        .build(),

                AclRelation.builder()
                        .objectId("readme")
                        .relation("editor")
                        .parents(
                                Set.of(
                                        AclRelationParent.builder()
                                                .relation("owner")
                                                .build()
                                )
                        )
                        .build(),

                AclRelation.builder()
                        .objectId("readme")
                        .relation("viewer")
                        .parents(
                                Set.of(
                                        AclRelationParent.builder()
                                                .relation("editor")
                                                .build()
                                )
                        )
                        .build()

                ));

        relationConfigRepository.save(relationConfig);

        log.info("{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(relationConfig));
    }

}
