package org.example.authserver;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.AuthService;
import org.example.authserver.service.CacheLoaderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@EnableTransactionManagement
@SpringBootApplication
public class Application {

  private final AuthService authService;
  private final CacheLoaderService cacheLoaderService;
  private final int grpcPort;

  public Application(
      AuthService authService,
      CacheLoaderService cacheLoaderService,
      @Value("${grpc.port:8080}") int grpcPort) {
    this.authService = authService;
    this.cacheLoaderService = cacheLoaderService;
    this.grpcPort = grpcPort;
  }

  @PostConstruct
  public void start() throws Exception {
    cacheLoaderService.subscribe();

    Server server = ServerBuilder.forPort(grpcPort).addService(authService).build();

    server.start();
    log.info("Started. Listen GRPC port: {}}", grpcPort);
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
