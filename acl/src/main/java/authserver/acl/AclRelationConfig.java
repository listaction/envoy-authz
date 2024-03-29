package authserver.acl;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclRelationConfig {

  @Builder.Default private UUID id = UUID.randomUUID();
  private String namespace;
  @Builder.Default private Set<AclRelation> relations = new HashSet<>();
}
