package com.example.splittest.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
  private boolean testMode;
  private int authzGrpcPort;
  private String authzHostname;
  private String authzRestApiUrl;
  private boolean aclsSubscribeEnabled;
  private boolean crsSubscribeEnabled;
}
