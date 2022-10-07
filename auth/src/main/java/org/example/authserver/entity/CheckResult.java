package org.example.authserver.entity;

import java.util.Collection;
import java.util.HashSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
