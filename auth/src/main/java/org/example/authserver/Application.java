package org.example.authserver;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.service.zanzibar.AclFilterService;
import org.example.authserver.service.AuthService;
import org.example.authserver.service.CacheLoaderService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootApplication
public class Application {

    private final AclFilterService aclFilterService;
    private final CacheLoaderService cacheLoaderService;
    private final ExampleDataset exampleDataset;

    public Application(AclFilterService aclFilterService, CacheLoaderService cacheLoaderService, ExampleDataset exampleDataset) {
        this.aclFilterService = aclFilterService;
        this.cacheLoaderService = cacheLoaderService;
        this.exampleDataset = exampleDataset;
    }

    @PostConstruct
    public void start() throws Exception {
        cacheLoaderService.subscribe();

        Server server = ServerBuilder.forPort(8080)
                .addService(new AuthService(aclFilterService))
                .build();

        server.start();
        log.info("Started. Listen post: 8080");
        exampleDataset.init();
        //server.awaitTermination();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
