package org.example.authserver;

import authserver.acl.Acl;
import authserver.acl.AclRelation;
import authserver.acl.AclRelationConfig;
import authserver.acl.AclRelationParent;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.SubscriptionRepository;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.model.RequestCache;
import org.example.authserver.service.zanzibar.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@Disabled
class ZanzibarImplTest {

    @Mock
    private AclRepository aclRepository;
    @Mock
    private AclRelationConfigRepository configRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private CacheService cacheService;

    private AclRelationConfigService aclRelationConfigService;
    private Zanzibar zanzibar;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aclRelationConfigService = new AclRelationConfigService(configRepository, cacheService, subscriptionRepository);
        zanzibar = new ZanzibarImpl(aclRepository, aclRelationConfigService);
    }

    @Test
    void check() {
        Map<String, Boolean> usersToTest = Map.of("user1_viewer", false, "user2_editor", true, "user3_owner", true);
        for (Map.Entry<String, Boolean> entry : usersToTest.entrySet()){
            String principal = entry.getKey();
            Set<Acl> acls = prepAcls();
            Set<Acl> aclsDocReadme = acls.stream()
                    .filter(acl->acl.getNamespace().equals("doc") && acl.getObject().equals("readme"))
                    .collect(Collectors.toSet());
            Set<Acl> aclsGroupDocument = acls.stream()
                    .filter(acl->acl.getNamespace().equals("group") && acl.getObject().equals("document"))
                    .collect(Collectors.toSet());
            AclRelationConfig config = prepConfig();

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(aclsDocReadme).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("doc"), eq("readme"), eq(principal));
            Mockito.doReturn(aclsDocReadme).when(aclRepository).findAllByPrincipalAndNsObjectIn(eq(principal), eq(List.of("doc:readme")));
            Mockito.doReturn(aclsGroupDocument).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("group"), eq("document"), eq(principal));
            Mockito.doReturn(aclsGroupDocument).when(aclRepository).findAllByPrincipalAndNsObjectIn(eq(principal), eq(List.of("group:document")));
            Mockito.doReturn(Set.of(config)).when(configRepository).findAll();
            Mockito.doReturn(Map.of(config.getNamespace(), config)).when(cacheService).getConfigs();
            aclRelationConfigService.update();

            CheckResult result = zanzibar.check("doc", "readme","allow_to_update", entry.getKey(), new RequestCache());
            System.out.printf("user %s [expected result: %s] => %s %n", entry.getKey(), entry.getValue(), result.isResult());
            assertEquals(result.isResult(), entry.getValue());
        }
    }

    @Test
    void relations() {
        Set<String> usersToTest = Set.of("user1_viewer", "user2_editor", "user3_owner");
        for (String principal : usersToTest) {

            Set<Acl> acls = prepAcls();
            Set<Acl> aclsDocReadme = acls.stream()
                    .filter(acl->acl.getNamespace().equals("doc") && acl.getObject().equals("readme"))
                    .collect(Collectors.toSet());
            Set<Acl> aclsGroupDocument = acls.stream()
                    .filter(acl->acl.getNamespace().equals("group") && acl.getObject().equals("document"))
                    .collect(Collectors.toSet());
            AclRelationConfig config = prepConfig();

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(aclsDocReadme).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("doc"), eq("readme"), eq(principal));
            Mockito.doReturn(aclsGroupDocument).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("group"), eq("document"), eq(principal));
            Mockito.doReturn(Set.of(config)).when(configRepository).findAll();

            Set<String> result = zanzibar.getRelations("doc", "readme", principal, new RequestCache());
            System.out.printf("user %s => %s %n", principal, result);

            // todo what do we assert here ?
        }
    }

    @Test
    void relationsSimple() {
        Map<String, String> usersToTest = Map.of("user1", "namespace:object#owner", "user2", "namespace:object#editor", "user3", "namespace:object#viewer");
        for (Map.Entry<String, String> entry : usersToTest.entrySet()) {
            String principal = entry.getKey();
            String expected = entry.getValue();
            Set<Acl> acls = new HashSet<>();
            acls.add(Acl.create("namespace:object#owner@user1"));
            acls.add(Acl.create("namespace:object#editor@user2"));
            acls.add(Acl.create("namespace:object#viewer@user3"));

            AclRelationConfig config = new AclRelationConfig();

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(acls).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("namespace"), eq("object"), eq(principal));
            Mockito.doReturn(acls).when(aclRepository).findAllByPrincipalAndNsObjectIn(eq(principal), eq(List.of("namespace:object")));
            Mockito.doReturn(acls).when(aclRepository).findAllByNsObjectIn(eq(List.of("namespace:object")));
            Mockito.doReturn(Set.of(config)).when(configRepository).findAll();
            Mockito.doReturn(Map.of("namespace:object", config)).when(cacheService).getConfigs();

            Set<String> result = zanzibar.getRelations("namespace", "object", principal, new RequestCache());
            System.out.printf("user %s => %s %n", principal, result);

            assertEquals(1, result.size());
            assertEquals(expected, new ArrayList<>(result).get(0));
        }
    }

    @Test
    void relationsContacts() {
        Map<String, String> usersToTest = Map.of("user1", "namespace:object#owner");
        for (Map.Entry<String, String> entry : usersToTest.entrySet()) {
            String principal = entry.getKey();
            String expected = entry.getValue();
            Set<Acl> acls = new HashSet<>();
            acls.add(Acl.create("contact:uuid1#owner@user1"));
            acls.add(Acl.create("contact:uuid1#editor@group:user#editor"));
            acls.add(Acl.create("contact:uuid1#viewer@user2"));

            Set<Acl> aclsGroupUser1 = acls.stream()
                    .filter(acl->acl.getNamespace().equals("group") && acl.getObject().equals("user1"))
                    .collect(Collectors.toSet());

            Set<Acl> aclsGroupUser2 = acls.stream()
                    .filter(acl->acl.getNamespace().equals("group") && acl.getObject().equals("user2"))
                    .collect(Collectors.toSet());

            Set<AclRelationConfig> configs = prepConfig2();

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(acls).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("contact"), eq("uuid1"), eq(principal));
            Mockito.doReturn(aclsGroupUser1).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("group"), eq("user1"), eq(principal));
            Mockito.doReturn(aclsGroupUser2).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("group"), eq("user2"), eq(principal));

            Mockito.doReturn(configs).when(configRepository).findAll();
            aclRelationConfigService.update();

            Set<String> result = zanzibar.getRelations("contact", "uuid1", principal, new RequestCache());
            System.out.printf("user %s => %s %n", principal, result);

            // todo what do we assert here ?
//            assert result.size() == 1;
//            assert new ArrayList<>(result).get(0).equals(expected);
        }
    }

    @Test
    void checkContactUsersTest() {
        Set<Acl> acls = new HashSet<>();
        acls.add(Acl.create("group:contactusers#viewer@user1")); // user1 has relation 'viewer' for group:contactuser
        acls.add(Acl.create("group:contactusers#viewer@user2")); // user2 has relation 'viewer' for group:contactuser
        acls.add(Acl.create("api:contact#enable@group:contactusers#viewer")); // access to 'contact' service allowed for every users from group:contactusers who has 'viewer' relation

        // user <-> expected result of check
        Map<String, Boolean> usersToTest = Map.of("user1", true, "user2", true, "user3", false);
        for (Map.Entry<String, Boolean> entry : usersToTest.entrySet()) {
            String principal = entry.getKey();
            boolean expected = entry.getValue();

            Set<Acl> aclsGroupContactusers = acls.stream()
                    .filter(acl->acl.getNamespace().equals("group") && acl.getObject().equals("contactusers"))
                    .collect(Collectors.toSet());

            Set<Acl> aclsApiContact = acls.stream()
                    .filter(acl->acl.getNamespace().equals("api") && acl.getObject().equals("contact"))
                    .collect(Collectors.toSet());

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(aclsGroupContactusers).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("group"), eq("contactusers"), eq(principal));
            Mockito.doReturn(aclsGroupContactusers).when(aclRepository).findAllByPrincipalAndNsObjectIn(eq(principal), eq(List.of("group:contactusers")));
            Mockito.doReturn(aclsApiContact).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("api"), eq("contact"), eq(principal));
            Mockito.doReturn(aclsApiContact).when(aclRepository).findAllByPrincipalAndNsObjectIn(eq(principal), eq(List.of("api:contact")));

            Mockito.doReturn(Set.of(new AclRelationConfig())).when(configRepository).findAll();

            // user1 and user2 are have access. And user3 is not.
            assertEquals(expected, zanzibar.check("api", "contact", "enable", principal, new RequestCache()).isResult());
        }
    }

    @Test
    void wildcardRelationsTest() {
        Set<Acl> acls = new HashSet<>();
        acls.add(Acl.create("group:contactusers#member@user1")); // user1 has relation 'member' for group:contactuser
        acls.add(Acl.create("group:contactusers#member@user2")); // user2 has relation 'member' for group:contactuser

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

        Map<String, AclRelationConfig> configMap = Map.of(relationConfigContact.getNamespace(), relationConfigContact);

        Mockito.doReturn(Set.of(relationConfigContact)).when(configRepository).findAll();
        Mockito.doReturn(configMap).when(cacheService).getConfigs();
        aclRelationConfigService.update();

        // user <-> expected result of check
        Map<String, Boolean> usersToTest = Map.of("user1", true, "user2", true, "user3", false);
        for (Map.Entry<String, Boolean> entry : usersToTest.entrySet()) {
            String principal = entry.getKey();
            boolean expected = entry.getValue();
            String uuid = UUID.randomUUID().toString();
            acls.add(Acl.create(String.format("contact:%s#owner@group:contactusers#member", uuid)));

            Set<Acl> aclsGroupContactusers = new ArrayList<>(acls).stream()
                    .filter(acl->acl.getNamespace().equals("group") && acl.getObject().equals("contactusers") && principal.equals(acl.getUser()))
                    .collect(Collectors.toSet());

            Set<Acl> aclsContact = new ArrayList<>(acls).stream()
                    .filter(acl->acl.getNamespace().equals("contact") && acl.getObject().equals(uuid))
                    .collect(Collectors.toSet());

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(aclsGroupContactusers).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("group"), eq("contactusers"), eq(principal));
            Mockito.doReturn(aclsGroupContactusers).when(aclRepository).findAllByPrincipalAndNsObjectIn(eq(principal), eq(List.of("group:contactusers")));
            Mockito.doReturn(aclsContact).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("contact"), eq(uuid), eq(principal));
            Mockito.doReturn(aclsContact).when(aclRepository).findAllByPrincipalAndNsObjectIn(eq(principal), eq(List.of(String.format("contact:%s", uuid))));

            System.out.println("user: " + principal);
            // user1 and user2 are have access. And user3 is not.
            assertEquals(expected, zanzibar.check("contact", uuid, "viewer", principal, new RequestCache()).isResult());
        }
    }

    @Test
    void exclusionTest() {
        Set<Acl> acls = new HashSet<>();
        acls.add(Acl.create("group:object#rel1@user1"));
        acls.add(Acl.create("group:object#rel2@user1"));

        acls.add(Acl.create("group:object#rel1@user2"));
        acls.add(Acl.create("group:object#rel3@user2"));

        AclRelationConfig relationConfigContact = new AclRelationConfig();
        relationConfigContact.setNamespace("group:object");
        relationConfigContact.setRelations(Set.of(
                AclRelation.builder()
                        .object("object")
                        .relation("rel1")
                        .exclusions(
                                Set.of("rel2")
                        )
                        .build(),
                AclRelation.builder()
                        .object("object")
                        .relation("rel2")
                        .exclusions(
                                Set.of("rel1")
                        )
                        .build()
        ));

        Map<String, AclRelationConfig> configMap = Map.of(relationConfigContact.getNamespace(), relationConfigContact);

        Mockito.doReturn(Set.of(relationConfigContact)).when(configRepository).findAll();
        Mockito.doReturn(configMap).when(cacheService).getConfigs();
        aclRelationConfigService.update();

        // user <-> expected result of check
        Map<String, Boolean> usersToTest = Map.of("user1", false, "user2", true);
        for (Map.Entry<String, Boolean> entry : usersToTest.entrySet()) {
            String principal = entry.getKey();
            boolean expected = entry.getValue();

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(acls).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("group"), eq("object"), anyString());
            Mockito.doReturn(acls).when(aclRepository).findAllByPrincipalAndNsObjectIn(anyString(), eq(List.of("group:object")));
            Mockito.doReturn(acls).when(aclRepository).findAllByNsObjectIn(eq(List.of("group:object")));

            System.out.println("user: " + principal);
            // user1 should be excluded, user2 should pass
            assertEquals(expected, zanzibar.check("group", "object", "rel1", principal, new RequestCache()).isResult());
        }
    }

    @Test
    void intersectionTest() {
        Set<Acl> acls = new HashSet<>();
        acls.add(Acl.create("group:object#rel1@user1"));
        acls.add(Acl.create("group:object#rel2@user1"));

        acls.add(Acl.create("group:object#rel1@user2"));
        acls.add(Acl.create("group:object#rel3@user2"));

        AclRelationConfig relationConfigContact = new AclRelationConfig();
        relationConfigContact.setNamespace("group:object");
        relationConfigContact.setRelations(Set.of(
                AclRelation.builder()
                        .object("object")
                        .relation("rel1")
                        .intersections(
                                Set.of("rel2")
                        )
                        .build(),
                AclRelation.builder()
                        .object("object")
                        .relation("rel2")
                        .intersections(
                                Set.of("rel1")
                        )
                        .build()
        ));

        Map<String, AclRelationConfig> configMap = Map.of(relationConfigContact.getNamespace(), relationConfigContact);

        Mockito.doReturn(Set.of(relationConfigContact)).when(configRepository).findAll();
        Mockito.doReturn(configMap).when(cacheService).getConfigs();
        aclRelationConfigService.update();

        // user <-> expected result of check
        Map<String, Boolean> usersToTest = Map.of("user1", true, "user2", false);
        for (Map.Entry<String, Boolean> entry : usersToTest.entrySet()) {
            String principal = entry.getKey();
            boolean expected = entry.getValue();

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(acls).when(aclRepository).findAllByNamespaceAndObjectAndUser(eq("group"), eq("object"), anyString());
            Mockito.doReturn(acls).when(aclRepository).findAllByPrincipalAndNsObjectIn(anyString(), eq(List.of("group:object")));
            Mockito.doReturn(acls).when(aclRepository).findAllByNsObjectIn(eq(List.of("group:object")));

            System.out.println("user: " + principal);
            // user1 should pass, user2 should be excluded
            assertEquals(expected, zanzibar.check("group", "object", "rel1", principal, new RequestCache()).isResult());
        }
    }

    @Test
    void parserTest() {
        List<String> list = new ArrayList<>();
        list.add("doc:readme#owner@10"); // User 10 is an owner of doc:readme
        list.add("group:eng#member@11"); // User 11 is a member of group:eng
        list.add("doc:readme#viewer@group:eng#member"); // Members of group:eng are viewers of doc:readme
        list.add("doc:readme#parent@folder:A#..."); //doc:readme is in folder:A

        for (String s : list) {
            Acl parsedAcl = Acl.create(s);

            System.out.println(parsedAcl);
        }
    }

    private AclRelationConfig prepConfig() {
        AclRelationConfig relationConfig = new AclRelationConfig();
        relationConfig.setNamespace("group:document");
        relationConfig.setRelations(Set.of(
                AclRelation.builder()
                        .object("document")
                        .relation("owner")
                        .build(),

                AclRelation.builder()
                        .object("document")
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
                        .object("document")
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

        return relationConfig;
    }

    private Set<Acl> prepAcls() {
        Set<Acl> acls = new HashSet<>();

        Acl acl1 = new Acl();
        acl1.setNamespace("doc");
        acl1.setObject("readme");
        acl1.setRelation("allow_to_update");
        acl1.setUser("*");
        acl1.setUsersetNamespace("group");
        acl1.setUsersetObject("document");
        acl1.setUsersetRelation("editor");

        Acl acl2 = new Acl();
        acl2.setNamespace("group");
        acl2.setObject("document");
        acl2.setRelation("viewer");
        acl2.setUser("user1_viewer");

        Acl acl3 = new Acl();
        acl3.setNamespace("group");
        acl3.setObject("document");
        acl3.setRelation("editor");
        acl3.setUser("user2_editor");

        Acl acl4 = new Acl();
        acl4.setNamespace("group");
        acl4.setObject("document");
        acl4.setRelation("owner");
        acl4.setUser("user3_owner");

        acls.add(acl1);
        acls.add(acl2);
        acls.add(acl3);
        acls.add(acl4);

        return acls;
    }

    private Set<AclRelationConfig> prepConfig2(){
        AclRelationConfig relationConfig = new AclRelationConfig();
        relationConfig.setNamespace("group:user1");
        relationConfig.setRelations(Set.of(
                AclRelation.builder()
                        .object("user1")
                        .relation("owner")
                        .build(),
                AclRelation.builder()
                        .object("user1")
                        .relation("editor")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("owner")
                                        .build()
                        ))
                        .build(),
                AclRelation.builder()
                        .object("user1")
                        .relation("viewer")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("editor")
                                        .build()
                        ))
                        .build()
        ));

        AclRelationConfig relationConfig2 = new AclRelationConfig();
        relationConfig2.setNamespace("group:user2");
        relationConfig2.setRelations(Set.of(
                AclRelation.builder()
                        .object("user2")
                        .relation("owner")
                        .build(),
                AclRelation.builder()
                        .object("user2")
                        .relation("editor")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("owner")
                                        .build()
                        ))
                        .build(),
                AclRelation.builder()
                        .object("user2")
                        .relation("viewer")
                        .parents(Set.of(
                                AclRelationParent.builder()
                                        .relation("editor")
                                        .build()
                        ))
                        .build()
        ));

        return Set.of(relationConfig, relationConfig2);
    }
}
