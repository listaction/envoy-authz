package org.example.authserver.config;

import org.example.authserver.repo.RelCacheRepository;
import org.example.authserver.service.CacheService;
import org.example.authserver.service.CacheServiceImpl;
import org.example.authserver.service.MeterService;
import org.example.authserver.service.StubCacheServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheStubConfig {
    @Bean
    @ConditionalOnProperty(value = "app.cache-mode", havingValue="true")
    public CacheService realCache(MeterService meterService, RelCacheRepository repository){
        return new CacheServiceImpl(meterService, repository);
    }

    @Bean
    @ConditionalOnProperty(value = "app.cache-mode", havingValue="false")
    public CacheService stubCache(MeterService meterService, RelCacheRepository repository){
        return new StubCacheServiceImpl();
    }
}
