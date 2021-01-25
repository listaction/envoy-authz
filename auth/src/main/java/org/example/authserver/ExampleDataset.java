package org.example.authserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import authserver.acl.Acl;
import authserver.acl.AclRelation;
import authserver.acl.AclRelationConfig;
import authserver.acl.AclRelationParent;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.example.authserver.repo.AclRepository;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


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

        // acls for admin service level
        aclRepository.save(Acl.create("api:acl#enable@acl_admin")); // contact_service's user who calls acl service to create acls for a new contacts

        // acls for service level
        aclRepository.save(Acl.create("api:contact#enable@group:contactusers#member")); //group

        // acls for object level
        aclRepository.save(Acl.create("acl:create#owner@acl_admin")); // allow to create acls for acl_admin
        aclRepository.save(Acl.create("contact:null#owner@group:contactusers#member")); //group permission to create contacts

        //group membership
        aclRepository.save(Acl.create("group:contactusers#member@user1"));
        aclRepository.save(Acl.create("group:contactusers#member@user2"));

        conn.publish(ACL_REDIS_KEY, "update");

        try {
            initRelationConfigs();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void initRelationConfigs() throws JsonProcessingException {
        AclRelationConfig relationConfig1 = new AclRelationConfig();
        relationConfig1.setNamespace("group:contactusers");
        relationConfig1.setRelations(Set.of(
                AclRelation.builder()
                        .object("contactusers")
                        .relation("owner")
                        .build(),
                AclRelation.builder()
                        .object("contactusers")
                        .relation("editor")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("owner")
                                .build()
                        ))
                        .build(),
                AclRelation.builder()
                        .object("contactusers")
                        .relation("viewer")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("editor")
                                        .build()
                        ))
                        .build(),
                AclRelation.builder()
                        .object("contactusers")
                        .relation("member")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("viewer")
                                        .build()
                        ))
                        .build()
                ));

        relationConfigRepository.save(relationConfig1);

        log.info("{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(relationConfig1));
    }

}
