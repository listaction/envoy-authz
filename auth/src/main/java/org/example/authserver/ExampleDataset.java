package org.example.authserver;

import authserver.acl.Acl;
import authserver.acl.AclRelation;
import authserver.acl.AclRelationConfig;
import authserver.acl.AclRelationParent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.zanzibar.AclRelationConfigRepository;
import org.example.authserver.service.zanzibar.AclRepository;
import org.example.authserver.service.zanzibar.AclRelationConfigService;
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
    private final AclRelationConfigService relationConfigService;

    public ExampleDataset(JedisPool jedis, AclRepository aclRepository, AclRelationConfigRepository relationConfigRepository, AclRelationConfigService relationConfigService) {
        this.jedis = jedis;
        this.aclRepository = aclRepository;
        this.relationConfigRepository = relationConfigRepository;
        this.relationConfigService = relationConfigService;
    }

    public void init() {
        log.info("Initialize example dataset");
        Jedis conn = jedis.getResource();
        conn.del(ACL_REDIS_KEY);

        // acls for admin service level
        aclRepository.save(Acl.create("api:acl#enable@acl_admin")); // contact_service's user who calls acl service to create acls for a new contacts

        // acls for service level
        aclRepository.save(Acl.create("api:contact#enable@group:contactusers#member")); //group
        aclRepository.save(Acl.create("group:contactusers#member@user1"));
        aclRepository.save(Acl.create("group:contactusers#member@user2"));

        // acls for object level
        aclRepository.save(Acl.create("acl:create#owner@acl_admin")); // allow to create acls for acl_admin
        aclRepository.save(Acl.create("contact:null#owner@group:contactusers#member")); //group permission to create contacts

//        aclRepository.save(Acl.create("group:user1#owner@user1")); // user1 is owner of group:user1
//        aclRepository.save(Acl.create("group:user1#editor@user2")); // user2 can edit docs of user1
//        aclRepository.save(Acl.create("group:user2#owner@user2"));  // user2 is owner of group:user2
//        aclRepository.save(Acl.create("group:user2#viewer@user1")); // user1 can only view docs of user2
//        aclRepository.save(Acl.create("contact:null#owner@user2")); // user2 can create new contacts

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

        AclRelationConfig relationConfigContact = new AclRelationConfig();
        relationConfigContact.setNamespace("contact:*");
        relationConfigContact.setRelations(Set.of(
                AclRelation.builder()
                        .object("*")
                        .relation("owner")
                        .build(),
                AclRelation.builder()
                        .object("*")
                        .relation("editor")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("owner")
                                        .build()
                        ))
                        .build(),
                AclRelation.builder()
                        .object("*")
                        .relation("viewer")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("editor")
                                        .build()
                        ))
                        .build()
        ));

        relationConfigService.save(relationConfig1);
        relationConfigService.save(relationConfigContact);

        log.info("{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(relationConfig1));
        log.info("{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(relationConfigContact));
    }

}
