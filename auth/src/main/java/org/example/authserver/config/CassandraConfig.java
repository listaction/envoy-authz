package org.example.authserver.config;

import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration
@EnableCassandraRepositories
public class CassandraConfig {

    @Bean
    public CqlSessionBuilderCustomizer authCustomizer(CassandraProperties properties) {
        return (builder) -> builder.withAuthCredentials(properties.getUsername(), properties.getPassword());
    }
}
