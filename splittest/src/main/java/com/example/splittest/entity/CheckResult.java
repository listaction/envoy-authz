package com.example.splittest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckResult {

  private String httpMethod;
  private String requestPath;
  private String rejectedWithMappingId;
  private boolean mappingsPresent;
  @Builder.Default private boolean jwtPresent = true;
  private boolean result;
  private boolean cacheHit;

  @Builder.Default private Collection<String> tags = new HashSet<>();
  @Builder.Default private Map<String, String> events = new HashMap<>();
  @Builder.Default private Map<String, Long> metrics = new HashMap<>();
  private String traceId;
  private String tenantId;
  private String userId;
  private String allowedTags;
}
