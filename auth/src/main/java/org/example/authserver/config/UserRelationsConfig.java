package org.example.authserver.config;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class UserRelationsConfig {

    private boolean enabled;
    private boolean updateOnAclChange;
    private int scheduledPeriodTime;
    private TimeUnit scheduledPeriodTimeUnit;
}
