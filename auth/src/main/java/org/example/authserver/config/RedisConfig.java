package org.example.authserver.config;

import java.time.Duration;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

  @Bean
  @ConditionalOnProperty(value = "redis.enabled", havingValue = "true")
  public JedisPool jedisPool(
      @Value("${redis.hostname}") String redisHost,
      @Value("${redis.port}") Integer redisPort,
      @Value("${redis.ssl}") Boolean sslEnabled,
      @Value("${redis.password}") String redisPassword) {

    if (Strings.isEmpty(redisPassword)) redisPassword = null;
    JedisPool pool =
        new JedisPool(buildPoolConfig(), redisHost, redisPort, 2000, redisPassword, sslEnabled);
    return pool;
  }

  private JedisPoolConfig buildPoolConfig() {
    final JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(128);
    poolConfig.setMaxIdle(128);
    poolConfig.setMinIdle(16);
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnReturn(true);
    poolConfig.setTestWhileIdle(true);
    poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60));
    poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
    poolConfig.setNumTestsPerEvictionRun(3);
    poolConfig.setBlockWhenExhausted(true);
    poolConfig.setJmxEnabled(false); // this is ok, spring will register it
    return poolConfig;
  }
}
