package authserver.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckTestDto {
  private CheckRequestDTO request;
  private boolean result;
  private Map<String, String> resultHeaders;

  @Builder.Default
  private ZonedDateTime time = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
}
