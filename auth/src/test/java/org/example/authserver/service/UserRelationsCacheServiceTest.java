package org.example.authserver.service;

import org.example.authserver.Tester;
import org.example.authserver.config.UserRelationsConfig;
import org.example.authserver.entity.UserRelationEntity;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.wildfly.common.Assert.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

@ExtendWith(MockitoExtension.class)
public class UserRelationsCacheServiceTest {

    @Mock
    private UserRelationRepository userRelationRepository;
    @Mock
    private AclRepository aclRepository;
    @Mock
    private Zanzibar zanzibar;
    @Mock
    private CacheService cacheService;

    private UserRelationsCacheService service;

    @BeforeEach
    void setUp() {
        UserRelationsConfig config = Tester.createTrueUserRelationsConfigConfig();

        aclRepository = Mockito.mock(AclRepository.class);
        UserRelationCacheBuilder builder = new UserRelationCacheBuilder(config, aclRepository, userRelationRepository, zanzibar, cacheService);

        service = new UserRelationsCacheService(builder, userRelationRepository, aclRepository);
    }

    @Test
    public void getRelations_whenMaxAclUpdateTimeIsLessThenUsersMaxAclUpdateTime_shouldReturnNoCache() {
        UserRelationEntity entity = UserRelationEntity.builder().maxAclUpdated(5L).relations(new HashSet<>()).build();

        Mockito.doReturn(7L).when(aclRepository).findMaxAclUpdatedByPrincipal("user1");
        Mockito.doReturn(Optional.of(entity)).when(userRelationRepository).findById(any());

        assertTrue(service.getRelations("user1").isEmpty());
    }

    @Test
    public void getRelations_whenMaxAclUpdateTimeIsEqualToUsersMaxAclUpdateTime_shouldReturnCache() {
        UserRelationEntity entity = UserRelationEntity.builder().maxAclUpdated(5L).relations(new HashSet<>()).build();

        Mockito.doReturn(5L).when(aclRepository).findMaxAclUpdatedByPrincipal("user1");
        Mockito.doReturn(Optional.of(entity)).when(userRelationRepository).findById(any());

        assertFalse(service.getRelations("user1").isEmpty());
    }

    @Test
    public void getRelations_whenMaxAclUpdateTimeIsMoreThenUsersMaxAclUpdateTime_shouldReturnCache() {
        UserRelationEntity entity = UserRelationEntity.builder().maxAclUpdated(7L).relations(new HashSet<>()).build();

        Mockito.doReturn(5L).when(aclRepository).findMaxAclUpdatedByPrincipal("user1");
        Mockito.doReturn(Optional.of(entity)).when(userRelationRepository).findById(any());

        assertFalse(service.getRelations("user1").isEmpty());
    }
}
