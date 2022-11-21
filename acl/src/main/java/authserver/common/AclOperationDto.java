package authserver.common;

import authserver.acl.Acl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclOperationDto {
  private Acl acl;
  private AclOperation op;
}
