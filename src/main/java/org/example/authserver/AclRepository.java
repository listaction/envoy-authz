package org.example.authserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class AclRepository {

    private static final String ACL_REDIS_KEY = "acls";

    private final String redisHost;

    public AclRepository(@Value("${redis.hostname}") String redisHost) {
        this.redisHost = redisHost;
    }

    @PostConstruct
    public void init(){
        Jedis conn = getRedisConnection();
        conn.del(ACL_REDIS_KEY);

        Acl acl1 = new Acl();
        acl1.setAllow(true);
        acl1.setResourceRegex("/service/1");
        acl1.setToken("foo");

        Acl acl2 = new Acl();
        acl2.setAllow(false);
        acl2.setResourceRegex("/service/1");
        acl2.setToken("bar");

        Acl acl3 = new Acl();
        acl3.setAllow(false);
        acl3.setResourceRegex("/service/2");
        acl3.setToken("foo");

        Acl acl4 = new Acl();
        acl4.setAllow(true);
        acl4.setResourceRegex("/service/2");
        acl4.setToken("bar");

        save(acl1);
        save(acl2);
        save(acl3);
        save(acl4);

        conn.publish(ACL_REDIS_KEY, "update");
    }

    public Set<Acl> findAll(){
        Jedis conn = getRedisConnection();
        List<String> jsons = conn.lrange(ACL_REDIS_KEY, 0, -1);

        return jsons.stream()
                .map(Utils::jsonToAcl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void save(Acl acl) {
        String json = Utils.aclToJson(acl);
        Jedis conn = getRedisConnection();
        conn.lpush(ACL_REDIS_KEY, json);
    }

    public void delete(Acl acl){

    }

    public Flux<String> subscribe(){
        return Flux.create(sink -> {
            Jedis conn = getRedisConnection();
            conn.subscribe(new AclListener(sink), ACL_REDIS_KEY);
        });
    }

    private Jedis getRedisConnection(){
        final JedisPoolConfig poolConfig = buildPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, redisHost);
        return jedisPool.getResource();
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
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
