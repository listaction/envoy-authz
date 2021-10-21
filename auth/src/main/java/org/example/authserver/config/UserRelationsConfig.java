package org.example.authserver.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRelationsConfig {

    private boolean enabled;
    private boolean updateOnAclChange;
}
