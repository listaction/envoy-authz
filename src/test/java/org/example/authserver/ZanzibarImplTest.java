package org.example.authserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.authserver.domain.Acl;
import org.example.authserver.domain.AclRelation;
import org.example.authserver.domain.AclRelationConfig;
import org.example.authserver.domain.AclRelationParent;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.service.AclRelationConfigService;
import org.example.authserver.service.Zanzibar;
import org.example.authserver.service.ZanzibarImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;

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
        Set<Acl> acls = prepAcls();
        AclRelationConfig config = prepConfig();

        Mockito.doReturn(acls).when(aclRepository).findAll();
        Mockito.doReturn(config).when(configRepository).findOneByNamespace(anyString());

        Map<String, Boolean> usersToTest = Map.of("user1_viewer", false, "user2_editor", true, "user3_owner", true);
        for (Map.Entry<String, Boolean> entry : usersToTest.entrySet()){
            boolean result = zanzibar.check("doc:readme", entry.getKey(), "editor");
            System.out.println(String.format("user %s [expected result: %s] => %s ", entry.getKey(), entry.getValue(), result));
            assert result == entry.getValue();
        }
    }

    private AclRelationConfig prepConfig() {
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

        return relationConfig;
    }

    private Set<Acl> prepAcls() {
        Set<Acl> acls = new HashSet<>();

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

        acls.add(acl1);
        acls.add(acl2);
        acls.add(acl3);

        return acls;
    }

}
