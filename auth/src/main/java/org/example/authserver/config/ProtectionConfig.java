package org.example.authserver.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProtectionConfig {
  private final AppProperties properties;

  public ProtectionConfig(AppProperties properties) {
    this.properties = properties;
  }

  @Bean
  FilterRegistrationBean<ApiKeyFilter> apiKeyFilter() {
    final FilterRegistrationBean<ApiKeyFilter> filterFilterRegistrationBean =
        new FilterRegistrationBean<>();
    filterFilterRegistrationBean.setFilter(new ApiKeyFilter(properties));
    filterFilterRegistrationBean.addUrlPatterns(
        "/mapping/create",
        "/mapping/create-many",
        "/mapping/clear",
        "/mapping/delete/**");

    return filterFilterRegistrationBean;
  }
}
