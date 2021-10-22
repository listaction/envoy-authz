package org.example.authserver;

import org.example.authserver.config.UserRelationsConfig;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Tester {

    private Tester() {
    }

    public static boolean waitFor(Supplier<Boolean> condition) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
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
}
