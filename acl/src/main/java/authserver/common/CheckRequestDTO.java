package authserver.common;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckRequestDTO {
  private String httpMethod;
  private String requestPath;
  private Map<String, String> headersMap;
  private String userId;
  private String tenant;
}
