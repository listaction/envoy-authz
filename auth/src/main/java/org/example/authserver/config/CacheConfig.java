package org.example.authserver.config;

import org.example.authserver.repo.RelCacheRepository;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.CacheServiceImpl;
import org.example.authserver.service.MeterService;
import org.example.authserver.service.StubCacheServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
  @Bean
  public CacheService realCache(
      AppProperties appProperties, MeterService meterService, RelCacheRepository repository) {
    if (appProperties.isCacheEnabled()) {
      return new CacheServiceImpl(meterService, repository);
    } else {
      return new StubCacheServiceImpl();
    }
  }
}
