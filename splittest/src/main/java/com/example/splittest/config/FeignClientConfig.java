package com.example.splittest.config;

import feign.Feign;
import feign.Logger;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {
  private static final OkHttpClient client = new OkHttpClient();

  @Bean
  public AuthzClient authzClient(AppProperties appProperties) {
    return Feign.builder()
        .client(client)
        .encoder(new JacksonEncoder())
        .decoder(new JacksonDecoder())
        .retryer(new Retryer.Default(1000, 50_000, 5))
        .logger(new Slf4jLogger())
        .logLevel(Logger.Level.BASIC)
        .target(AuthzClient.class, appProperties.getAuthzRestApiUrl());
  }
}
