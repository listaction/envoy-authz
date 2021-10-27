package org.example.authserver;

import org.assertj.core.util.Sets;
import org.example.authserver.config.UserRelationsConfig;
import org.example.authserver.service.model.RequestCache;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Tester {

    private Tester() {
    }

    public static boolean waitFor(Supplier<Boolean> condition) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            if (condition.get()) {
                return true;
            }
            Thread.sleep(1);
        }
        return false;
    }

    public static UserRelationsConfig createTrueUserRelationsConfigConfig() {
        return createUserRelationsConfig(true);
    }

    public static UserRelationsConfig createUserRelationsConfig(boolean enabled) {
        UserRelationsConfig config = new UserRelationsConfig();
        config.setEnabled(enabled);
        config.setUpdateOnAclChange(true);
        config.setScheduledPeriodTime(1);
        config.setScheduledPeriodTimeUnit(TimeUnit.MINUTES);
        return config;
    }

    public static RequestCache createTestCache() {
        RequestCache requestCache = new RequestCache();
        requestCache.getPrincipalHighCardinalityCache().put("user1", Sets.newHashSet());
        requestCache.getPrincipalHighCardinalityCache().put("warm up", Sets.newHashSet());
        return requestCache;
    }
}
