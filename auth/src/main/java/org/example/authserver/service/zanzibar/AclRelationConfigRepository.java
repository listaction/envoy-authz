package org.example.authserver.service.zanzibar;

import authserver.acl.AclRelationConfig;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.Utils;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.example.authserver.config.Constants.ACL_REL_CONFIG_REDIS_KEY;

@Slf4j
@Repository
public class AclRelationConfigRepository {

    private final JedisPool jedis;

    public AclRelationConfigRepository(JedisPool jedis) {
        this.jedis = jedis;
    }

    public Set<AclRelationConfig> findAll(){
        Jedis conn = jedis.getResource();
        List<String> jsons = conn.lrange(ACL_REL_CONFIG_REDIS_KEY, 0, -1);
        conn.close();
        return jsons.stream()
                .map(Utils::jsonToConfig)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    public void save(AclRelationConfig config) {
        String json = Utils.configToJson(config);
        Jedis conn = jedis.getResource();
        conn.lpush(ACL_REL_CONFIG_REDIS_KEY, json);
        conn.close();

        publish();
    }

    public void publish() {
        Jedis conn = jedis.getResource();
        conn.publish(ACL_REL_CONFIG_REDIS_KEY, UUID.randomUUID().toString());
        conn.close();
    }

    public void delete(AclRelationConfig acl){

    }

    public Flux<String> subscribe(){
        return Flux.create(sink -> {
            Jedis conn = jedis.getResource();
            conn.subscribe(new AclRelationConfigListener(sink), ACL_REL_CONFIG_REDIS_KEY);
        });
    }

    private static class AclRelationConfigListener extends JedisPubSub {

        private final FluxSink<String> fluxSink;

        public AclRelationConfigListener(FluxSink<String> fluxSink) {
            this.fluxSink = fluxSink;
        }

        @Override
        public void onMessage(String channel, String message) {
            super.onMessage(channel, message);
            fluxSink.next(message);
        }
    }

}
