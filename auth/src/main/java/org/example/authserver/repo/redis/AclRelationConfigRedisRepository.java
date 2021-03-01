package org.example.authserver.repo.redis;

import authserver.acl.AclRelationConfig;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.Utils;
import org.example.authserver.repo.AclRelationConfigRepository;
import org.example.authserver.repo.SubscriptionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.authserver.config.Constants.ACL_REL_CONFIG_REDIS_KEY;

@Slf4j
@Configuration
@ConditionalOnProperty(
        value="app.database",
        havingValue = "REDIS"
)
public class AclRelationConfigRedisRepository implements AclRelationConfigRepository {

    private final JedisPool jedis;
    private final SubscriptionRepository subscriptionRepository;

    public AclRelationConfigRedisRepository(JedisPool jedis, SubscriptionRepository subscriptionRepository) {
        this.jedis = jedis;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Set<AclRelationConfig> findAll(){
        Jedis conn = jedis.getResource();
        Collection<String> jsons = conn.hgetAll(ACL_REL_CONFIG_REDIS_KEY).values();
        conn.close();
        return jsons.stream()
                .map(Utils::jsonToConfig)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public AclRelationConfig findOneById(String id) {
        Jedis conn = jedis.getResource();
        String json = conn.hget(ACL_REL_CONFIG_REDIS_KEY, id);
        conn.close();
        return Utils.jsonToConfig(json);
    }

    @Override
    public void save(AclRelationConfig config) {
        String json = Utils.configToJson(config);
        Jedis conn = jedis.getResource();
        conn.hset(ACL_REL_CONFIG_REDIS_KEY, config.getId().toString(), json);
        conn.close();

        subscriptionRepository.publish(config);
    }

    @Override
    public void delete(AclRelationConfig config){
        Jedis conn = jedis.getResource();
        conn.hdel(ACL_REL_CONFIG_REDIS_KEY, config.getId().toString());
        conn.publish(ACL_REL_CONFIG_REDIS_KEY, config.getId().toString());
        conn.close();
    }

}
