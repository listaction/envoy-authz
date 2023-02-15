package org.example.authserver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
  private boolean copyModeEnabled;
  private boolean cacheEnabled;
  private String jwtParam;
  private boolean jwtParamEnabled;
  private boolean accessTokenCookieEnabled;
  private String accessTokenCookie;
  private boolean tokenSignOutCheckEnabled;
}
