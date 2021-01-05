package org.example.authserver;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
public class Application {

    private final AclFilterService aclFilterService;
    private final CacheLoaderService cacheLoaderService;

    public Application(AclFilterService aclFilterService, CacheLoaderService cacheLoaderService) {
        this.aclFilterService = aclFilterService;
        this.cacheLoaderService = cacheLoaderService;
    }

    @PostConstruct
    public void start() throws Exception {
        cacheLoaderService.subscribe();

        Server server = ServerBuilder.forPort(8080)
                .addService(new AuthService(aclFilterService))
                .build();

        server.start();
        server.awaitTermination();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
