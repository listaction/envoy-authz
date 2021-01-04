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

    @PostConstruct
    public void start() throws Exception {
        Server server = ServerBuilder.forPort(8080)
                .addService(new AuthService())
                .build();

        server.start();
        server.awaitTermination();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
