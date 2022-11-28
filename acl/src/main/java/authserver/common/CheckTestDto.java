package authserver.common;

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
  private ZonedDateTime time;
}
