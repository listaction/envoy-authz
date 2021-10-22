package org.example.authserver.service;

import com.google.common.collect.Sets;
import org.example.authserver.Tester;
import org.example.authserver.config.AppProperties;
import org.example.authserver.config.UserRelationsConfig;
import org.example.authserver.entity.UserRelationEntity;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class RelationsServiceTest {

    @Mock
    private AclRepository aclRepository;
    @Mock
    private UserRelationRepository userRelationRepository;
    @Mock
    private Zanzibar zanzibar;
    @Mock
    private AppProperties appProperties;

    private UserRelationCacheBuilder builder;
    private UserRelationsCacheService cacheService;
    private RelationsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        UserRelationsConfig config = Tester.createTrueUserRelationsConfigConfig();

        Mockito.doReturn(config).when(appProperties).getUserRelationsCache();

        builder = new UserRelationCacheBuilder(config, aclRepository, userRelationRepository, zanzibar);
        builder.build("warm up"); // warm up executor

        cacheService = new UserRelationsCacheService(builder, userRelationRepository);
        service = new RelationsService(zanzibar, cacheService);
    }

    @Test
    public void getRelations_whenCacheBuilderIsBuilding_shouldCallZanzibar() throws InterruptedException {
        Mockito.doReturn(Sets.newHashSet("user1")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1")).when(aclRepository).findAllObjects();

        assertTrue(builder.buildAsync("user1"));
        assertTrue(Tester.waitFor(() -> builder.isInProgress()));

        service.getRelations("ns1", "obj1", "user1", new HashMap<>(), new HashMap<>());

        assertTrue(Tester.waitFor(() -> !builder.isInProgress()));
        // 2 -> 1 time during building cache + 1 time for fetching data
        Mockito.verify(zanzibar, Mockito.times(2)).getRelations(any(), any(), any(), any(), any());
    }

    @Test
    public void getRelations_whenCacheIsBuilt_shouldReturnCacheAndDontCallZanzibar() throws InterruptedException {
        Mockito.doReturn(Sets.newHashSet("user1")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1")).when(aclRepository).findAllObjects();
        Mockito.doReturn(Optional.of(UserRelationEntity.builder().relations(new HashSet<>()).build())).when(userRelationRepository).findById("user1");

        assertTrue(builder.buildAsync("user1"));
        assertTrue(Tester.waitFor(() -> builder.isInProgress()));
        assertTrue(Tester.waitFor(() -> !builder.isInProgress()));
        assertTrue(Tester.waitFor(() -> builder.canUseCache("user1")));

        service.getRelations("ns1", "obj1", "user1", new HashMap<>(), new HashMap<>());

        // expected exactly 1 call for cache building and then service call should take cached data
        Mockito.verify(zanzibar, Mockito.times(1)).getRelations(any(), any(), any(), any(), any());
    }
}
