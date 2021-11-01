package org.example.authserver.service;

import com.google.common.collect.Sets;
import org.example.authserver.Tester;
import org.example.authserver.entity.UserRelationEntity;
import org.example.authserver.repo.AclRepository;
import org.example.authserver.repo.pgsql.UserRelationRepository;
import org.example.authserver.service.model.RequestCache;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class UserRelationCacheBuilderTest {

    @Mock
    private AclRepository aclRepository;
    @Mock
    private UserRelationRepository userRelationRepository;
    @Mock
    private Zanzibar zanzibar;
    @Mock
    private CacheService cacheService;

    private UserRelationCacheBuilder builder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Mockito.doReturn(Tester.createTestCache()).when(cacheService).prepareHighCardinalityCache(any());

        builder = new UserRelationCacheBuilder(Tester.createTrueUserRelationsConfigConfig(), aclRepository, userRelationRepository, zanzibar, cacheService);
        builder.build("warm up"); // warm up executor
    }

    @Disabled
    @Test
    public void createUserRelations_whenInvoked_shouldSaveOnlyLowCardinalityRelations() {
        builder = new UserRelationCacheBuilder(Tester.createTrueUserRelationsConfigConfig(), aclRepository, userRelationRepository, zanzibar, cacheService);

        RequestCache requestCache = new RequestCache();
        requestCache.getPrincipalHighCardinalityCache().put("user1", Sets.newHashSet("test-application:ID-applicationinstance___8607b629-f1d6-4ab3-99b4-236ceac07371#Owner", "test:groups#TB"));

        Mockito.doReturn(1L).when(aclRepository).findMaxAclUpdatedByPrincipal("user1");
        Mockito.doReturn(requestCache).when(cacheService).prepareHighCardinalityCache(any());
        Mockito.doReturn(Sets.newHashSet("test:coarse-access#TB", "test:groups#TB", "test-application:ID-applicationinstance___8607b629-f1d6-4ab3-99b4-236ceac07371#Owner")).when(zanzibar).getRelations(any(), any(), any(), any());

        UserRelationEntity entity = builder.createUserRelations("user1", Sets.newHashSet("ns1", "ns2", "ns3"), Sets.newHashSet("obj1", "obj2", "obj3")).get();

        assertEquals(1, entity.getRelations().size());
        assertEquals("test:coarse-access#TB", entity.getRelations().iterator().next());
        assertEquals("user1", entity.getUser());
    }

    @Test
    public void fullRebuildAsync_whenInvokedManyTimes_shouldSkipSubsequentCalls() throws InterruptedException {
        Mockito.doReturn(Sets.newHashSet("user1", "user2", "user3")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1", "ns2", "ns3")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1", "obj2", "obj3")).when(aclRepository).findAllObjects();
        Mockito.doAnswer(new AnswersWithDelay(1, new Returns(new HashSet<>()))).when(zanzibar).getRelations(any(), any(), any(), any());

        assertTrue(builder.fullRebuildAsync());
        Thread.sleep(15); // executors.execute() takes time

        assertFalse(builder.fullRebuildAsync());
        assertFalse(builder.fullRebuildAsync());
    }

    @Test
    public void build_whenScheduleSameUserForUpdate_shouldBuildItInitiallyAndPutItInQueueForLaterRebuild() throws InterruptedException {
        Mockito.doReturn(Sets.newHashSet("user1", "user2", "user3")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1", "ns2", "ns3")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1", "obj2", "obj3")).when(aclRepository).findAllObjects();
        Mockito.doAnswer(new AnswersWithDelay(1, new Returns(new HashSet<>()))).when(zanzibar).getRelations(any(), any(), any(), any());

        assertTrue(builder.buildAsync("user1"));
        assertTrue(Tester.waitFor(() -> builder.isInProgress()));

        // all subsequent calls should not build but schedule user
        assertFalse(builder.build("user1"));
        assertFalse(builder.build("user1"));

        assertTrue(builder.hasInProgress("user1"));
        assertTrue(builder.hasScheduled("user1"));
    }

    @Test
    public void firstTimeBuild_whenInvokedFistTime_shouldBuildCache() {
        Mockito.doReturn(Sets.newHashSet("user1")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1")).when(aclRepository).findAllObjects();

        Mockito.doReturn(0L).when(userRelationRepository).count();

        assertTrue(builder.firstTimeBuild());
    }

    @Test
    public void firstTimeBuild_whenInvokedForFilledCache_shouldSkipExecution() {
        Mockito.doReturn(Sets.newHashSet("user1")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1")).when(aclRepository).findAllObjects();

        Mockito.doReturn(1L).when(userRelationRepository).count();

        assertFalse(builder.firstTimeBuild());
    }

    @Test
    public void buildAll_whenCacheEnabledIsFalse_shouldReturnFalse() {
        UserRelationCacheBuilder b = new UserRelationCacheBuilder(Tester.createUserRelationsConfig(false), aclRepository, userRelationRepository, zanzibar, cacheService);
        assertFalse(b.buildAll());
    }

    @Test
    public void buildAll_whenNoUsersFound_shouldReturnFalse() {
        Mockito.doReturn(Sets.newHashSet("ns1")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1")).when(aclRepository).findAllObjects();

        assertFalse(builder.buildAll());
    }

    @Test
    public void buildAll_whenNoNamespacesFound_shouldReturnFalse() {
        Mockito.doReturn(Sets.newHashSet("user1")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("obj1")).when(aclRepository).findAllObjects();

        assertFalse(builder.buildAll());
    }

    @Test
    public void buildAll_whenNoObjectsFound_shouldReturnFalse() {
        Mockito.doReturn(Sets.newHashSet("user1")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1")).when(aclRepository).findAllNamespaces();

        assertFalse(builder.buildAll());
    }

    @Test
    public void buildAll_whenAllRequiredDataAreGood_shouldReturnFalse() {
        Mockito.doReturn(Sets.newHashSet("user1")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1")).when(aclRepository).findAllObjects();

        assertTrue(builder.buildAll());
    }

    @Test
    public void canUseCache_whenCacheIsDisabled_shouldReturnFalse() {
        UserRelationCacheBuilder b = new UserRelationCacheBuilder(Tester.createUserRelationsConfig(false), aclRepository, userRelationRepository, zanzibar, cacheService);
        assertFalse(b.canUseCache("user1"));
    }

    @Test
    public void canUseCache_whenCacheBuildingIsInProgress_shouldReturnFalse() throws InterruptedException {
        Mockito.doReturn(Sets.newHashSet("user1")).when(aclRepository).findAllEndUsers();
        Mockito.doReturn(Sets.newHashSet("ns1")).when(aclRepository).findAllNamespaces();
        Mockito.doReturn(Sets.newHashSet("obj1")).when(aclRepository).findAllObjects();
        Mockito.doAnswer(new AnswersWithDelay(1, new Returns(new HashSet<>()))).when(zanzibar).getRelations(any(), any(), any(), any());

        assertTrue(builder.buildAsync("user1"));
        assertTrue(Tester.waitFor(() -> builder.isInProgress()));
        assertFalse(builder.canUseCache("user1"));

        assertTrue(Tester.waitFor(() -> !builder.isInProgress()));
        assertTrue(builder.canUseCache("user1"));
    }
}
