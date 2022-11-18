package org.example.authserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckResult {
  private String rejectedWithMappingId;
  private boolean mappingsPresent;
  @Builder.Default private boolean jwtPresent = true;
  private boolean result;
  @Builder.Default private Collection<String> tags = new HashSet<>();
  @Builder.Default private Map<String, String> events = new HashMap<>();
  @Builder.Default private String traceId = Strings.EMPTY;
  @Builder.Default private String spanId = Strings.EMPTY;

  public String eventsToJson() throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("trace_id", traceId);
    jsonObject.put("span_id", spanId);
    jsonObject.put("events", events);

    return jsonObject.toString();

  }
}
