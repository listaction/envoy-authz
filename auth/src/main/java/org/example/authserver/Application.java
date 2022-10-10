package org.example.authserver;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.service.AuthService;
import org.example.authserver.service.CacheLoaderService;
import org.example.authserver.service.RedisService;
import org.example.authserver.service.zanzibar.AclFilterService;
import org.example.authserver.service.zanzibar.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import redis.clients.jedis.JedisPool;

@Slf4j
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EnableTransactionManagement
@SpringBootApplication
public class Application {

  private final AclFilterService aclFilterService;
  private final RedisService redisService;
  private final CacheLoaderService cacheLoaderService;
  private final AppProperties appProperties;
  private final int grpcPort;
  private final TokenService tokenService;

  public Application(
      @Nullable JedisPool jedisPool,
      AclFilterService aclFilterService,
      CacheLoaderService cacheLoaderService,
      AppProperties appProperties,
      @Value("${grpc.port:8080}") int grpcPort,
      TokenService tokenService) {
    this.aclFilterService = aclFilterService;
    this.redisService = new RedisService(jedisPool);
    this.tokenService = tokenService;
    this.cacheLoaderService = cacheLoaderService;
    this.appProperties = appProperties;
    this.grpcPort = grpcPort;
  }

  @PostConstruct
  public void start() throws Exception {
    cacheLoaderService.subscribe();

    Server server =
        ServerBuilder.forPort(grpcPort)
            .addService(new AuthService(aclFilterService, redisService, tokenService))
            .build();

    server.start();
    log.info("Started. Listen post: {}}", grpcPort);
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
