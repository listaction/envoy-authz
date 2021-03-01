package org.example.authserver.repo;

import authserver.acl.Acl;
import authserver.acl.AclRelationConfig;
import reactor.core.publisher.Flux;

public interface SubscriptionRepository {

    void publish(Acl acl);

    void publish(AclRelationConfig config);

    Flux<String> subscribeAcl();

    Flux<String> subscribeConfig();

}
