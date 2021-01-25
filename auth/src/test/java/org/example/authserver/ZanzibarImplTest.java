package org.example.authserver;

import authserver.acl.Acl;
import authserver.acl.AclRelation;
import authserver.acl.AclRelationConfig;
import authserver.acl.AclRelationParent;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.service.AclRelationConfigService;
import org.example.authserver.service.Zanzibar;
import org.example.authserver.service.ZanzibarImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class ZanzibarImplTest {

    @Mock
    private AclRepository aclRepository;
    @Mock
    private AclRelationConfigRepository configRepository;

    private AclRelationConfigService aclRelationConfigService;
    private Zanzibar zanzibar;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aclRelationConfigService = new AclRelationConfigService(configRepository);
        zanzibar = new ZanzibarImpl(aclRepository, aclRelationConfigService);
    }

    @Test
    void check() {
        Map<String, Boolean> usersToTest = Map.of("user1_viewer", false, "user2_editor", true, "user3_owner", true);
        for (Map.Entry<String, Boolean> entry : usersToTest.entrySet()){

            Set<Acl> acls = prepAcls();
            Set<Acl> aclsDocReadme = acls.stream()
                    .filter(acl->acl.getNamespace().equals("doc") && acl.getObject().equals("readme"))
                    .collect(Collectors.toSet());
            Set<Acl> aclsGroupDocument = acls.stream()
                    .filter(acl->acl.getNamespace().equals("group") && acl.getObject().equals("document"))
                    .collect(Collectors.toSet());
            AclRelationConfig config = prepConfig();

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(aclsDocReadme).when(aclRepository).findAllByNamespaceAndObject(eq("doc"), eq("readme"));
            Mockito.doReturn(aclsGroupDocument).when(aclRepository).findAllByNamespaceAndObject(eq("group"), eq("document"));
            Mockito.doReturn(config).when(configRepository).findOneByNamespace(anyString());


            boolean result = zanzibar.check("doc", "readme","allow_to_update", entry.getKey());
            System.out.println(String.format("user %s [expected result: %s] => %s ", entry.getKey(), entry.getValue(), result));
            assert result == entry.getValue();
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
            Mockito.doReturn(aclsDocReadme).when(aclRepository).findAllByNamespaceAndObject(eq("doc"), eq("readme"));
            Mockito.doReturn(aclsGroupDocument).when(aclRepository).findAllByNamespaceAndObject(eq("group"), eq("document"));
            Mockito.doReturn(config).when(configRepository).findOneByNamespace(anyString());

            Set<String> result = zanzibar.getRelations("doc", "readme", principal);
            System.out.println(String.format("user %s => %s ", principal, result));
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
            Mockito.doReturn(acls).when(aclRepository).findAllByNamespaceAndObject(eq("namespace"), eq("object"));
            Mockito.doReturn(config).when(configRepository).findOneByNamespace(anyString());

            Set<String> result = zanzibar.getRelations("namespace", "object", principal);
            System.out.println(String.format("user %s => %s ", principal, result));
            assert result.size() == 1;
            assert new ArrayList<>(result).get(0).equals(expected);
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

            AclRelationConfig config = prepConfig2();

            Mockito.doReturn(acls).when(aclRepository).findAll();
            Mockito.doReturn(acls).when(aclRepository).findAllByNamespaceAndObject(eq("contact"), eq("uuid1"));
            Mockito.doReturn(aclsGroupUser1).when(aclRepository).findAllByNamespaceAndObject(eq("group"), eq("user1"));
            Mockito.doReturn(aclsGroupUser2).when(aclRepository).findAllByNamespaceAndObject(eq("group"), eq("user2"));

            Mockito.doReturn(config).when(configRepository).findOneByNamespace(anyString());

            Set<String> result = zanzibar.getRelations("contact", "uuid1", principal);
            System.out.println(String.format("user %s => %s ", principal, result));
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
            Mockito.doReturn(aclsGroupContactusers).when(aclRepository).findAllByNamespaceAndObject(eq("group"), eq("contactusers"));
            Mockito.doReturn(aclsApiContact).when(aclRepository).findAllByNamespaceAndObject(eq("api"), eq("contact"));

            Mockito.doReturn(new AclRelationConfig()).when(configRepository).findOneByNamespace(anyString());

            // user1 and user2 are have access. And user3 is not.
            assert zanzibar.check("api", "contact", "enable", principal) == expected;
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
        relationConfig.setNamespace("group");
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

    private AclRelationConfig prepConfig2(){
        AclRelationConfig relationConfig = new AclRelationConfig();
        relationConfig.setNamespace("group");
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
                        .build(),


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
        return relationConfig;
    }

}
