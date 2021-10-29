package org.example.authserver.service.zanzibar;

import lombok.extern.slf4j.Slf4j;
import org.example.authserver.Utils;

import authserver.acl.Acl;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class AclRepository {

    private static final String ACL_REDIS_KEY = "acls";

    private final JedisPool jedis;

    public AclRepository(@Nullable JedisPool jedis) {
        this.jedis = jedis;
    }

    public Set<Acl> findAll(){
        if (jedis == null) return null;
        Jedis conn = jedis.getResource();
        List<String> jsons = conn.lrange(ACL_REDIS_KEY, 0, -1);
        conn.close();
        return jsons.stream()
                .map(Utils::jsonToAcl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<Acl> findAllByNamespaceAndObjectAndPrincipal(String namespace, String object, String principal) {
        Set<Acl> allAcls = findAll();
        return allAcls.stream()
                .filter(acl->acl.getNamespace().equals(namespace))
                .filter(acl->acl.getObject().equals(object))
                .filter(f->(f.hasUserset() || (!f.hasUserset() && principal.equals(f.getUser()))))
                .collect(Collectors.toSet());
    }

    public void save(Acl acl) {
        if (jedis == null) return;
        String json = Utils.aclToJson(acl);
        Jedis conn = jedis.getResource();
        conn.lpush(ACL_REDIS_KEY, json);
        conn.close();
    }

    public void publish() {
        if (jedis == null) return;
        Jedis conn = jedis.getResource();
        conn.publish(ACL_REDIS_KEY, UUID.randomUUID().toString());
        conn.close();
    }

    public void delete(Acl acl){

    }

    public Flux<String> subscribe(){
        return Flux.create(sink -> {
            Jedis conn = jedis.getResource();
            conn.subscribe(new AclListener(sink), ACL_REDIS_KEY);
        });
    }

    private static class AclListener extends JedisPubSub {

        private final FluxSink<String> fluxSink;

        public AclListener(FluxSink<String> fluxSink) {
            this.fluxSink = fluxSink;
        }

        @Override
        public void onMessage(String channel, String message) {
            super.onMessage(channel, message);
            fluxSink.next(message);
        }
    }
}
