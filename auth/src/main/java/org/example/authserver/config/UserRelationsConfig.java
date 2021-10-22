package org.example.authserver.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Builder
public class UserRelationsConfig {

    private boolean enabled;
    private boolean updateOnAclChange;
    private int scheduledPeriodTime;
    private TimeUnit scheduledPeriodTimeUnit;
}
