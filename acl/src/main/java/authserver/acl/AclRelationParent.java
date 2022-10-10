package authserver.acl;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclRelationParent {

  private String relation;
  private Set<AclRelationParent> parents;
}
