package org.example.authserver;

import static org.example.authserver.config.Constants.ACL_REDIS_KEY;
import static org.example.authserver.config.Constants.ACL_REL_CONFIG_REDIS_KEY;

import authserver.acl.Acl;
import authserver.acl.AclRelation;
import authserver.acl.AclRelationConfig;
import authserver.acl.AclRelationParent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.AclService;
import org.example.authserver.service.zanzibar.AclRelationConfigService;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
@Service
public class ExampleDataset {

  private static final ObjectMapper mapper = new ObjectMapper();

  private final JedisPool jedis;
  private final AclService aclRepository;
  private final AclRelationConfigService relationConfigService;

  public ExampleDataset(
      @Nullable JedisPool jedis,
      AclService aclRepository,
      AclRelationConfigService relationConfigService) {
    this.jedis = jedis;
    this.aclRepository = aclRepository;
    this.relationConfigService = relationConfigService;
  }

  public void init() {
    if (jedis == null) return;
    log.info("Initialize example dataset");
    Jedis conn = jedis.getResource();
    conn.del(ACL_REDIS_KEY);
    conn.del(ACL_REL_CONFIG_REDIS_KEY);
    conn.del("idx_namespace");
    conn.del("idx_user");

    // acls for admin service level
    aclRepository.save(
        Acl.create(
            "api:acl#enable@acl_admin")); // contact_service's user who calls acl service to create
    // acls for a new contacts

    // acls for service level
    aclRepository.save(Acl.create("api:contact#enable@group:contactusers#member")); // group
    aclRepository.save(Acl.create("group:contactusers#admin@user1"));
    aclRepository.save(Acl.create("group:contactusers#admin@user2"));
    aclRepository.save(Acl.create("group:contactusers#editor@user3"));

    // acls for object level
    aclRepository.save(
        Acl.create("acl:create#owner@acl_admin")); // allow to create acls for acl_admin
    aclRepository.save(
        Acl.create(
            "contact:null#owner@group:contactusers#admin")); // group permission to create contacts

    try {
      initRelationConfigs();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  private void initRelationConfigs() throws JsonProcessingException {
    AclRelationConfig relationConfig1 = new AclRelationConfig();
    relationConfig1.setNamespace("group:contactusers");
    relationConfig1.setRelations(
        Set.of(
            AclRelation.builder().object("contactusers").relation("admin").build(),
            AclRelation.builder()
                .object("contactusers")
                .relation("editor")
                .parents(Set.of(AclRelationParent.builder().relation("admin").build()))
                .build(),
            AclRelation.builder()
                .object("contactusers")
                .relation("viewer")
                .parents(Set.of(AclRelationParent.builder().relation("editor").build()))
                .build(),
            AclRelation.builder()
                .object("contactusers")
                .relation("member")
                .parents(Set.of(AclRelationParent.builder().relation("viewer").build()))
                .build()));

    AclRelationConfig relationConfigContact = new AclRelationConfig();
    relationConfigContact.setNamespace("contact:*");
    relationConfigContact.setRelations(
        Set.of(
            AclRelation.builder().object("*").relation("owner").build(),
            AclRelation.builder()
                .object("*")
                .relation("editor")
                .parents(Set.of(AclRelationParent.builder().relation("owner").build()))
                .build(),
            AclRelation.builder()
                .object("*")
                .relation("viewer")
                .parents(Set.of(AclRelationParent.builder().relation("editor").build()))
                .build()));

    relationConfigService.save(relationConfig1);
    relationConfigService.save(relationConfigContact);

    log.info("{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(relationConfig1));
    log.info(
        "{}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(relationConfigContact));
  }
}
