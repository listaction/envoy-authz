package org.example.authserver.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
  @Builder.Default private String traceId = Strings.EMPTY;
  @Builder.Default private String tenantId = Strings.EMPTY;
  @Builder.Default private String allowedTags = Strings.EMPTY;

  public Map<String, Object> getResultMap() {
    Map<String, Object> resultMap = new HashMap<>();
    resultMap.put("http_method", httpMethod);
    resultMap.put("request_path", requestPath);
    resultMap.put("trace_id", traceId);
    resultMap.put("tenant_id", tenantId);
    resultMap.put("events", events);
    resultMap.put("metrics", metrics);
    resultMap.put("allowedTags", allowedTags);
    resultMap.put("result", result);
    resultMap.put("cacheHit", cacheHit);

    return resultMap;
  }
}
