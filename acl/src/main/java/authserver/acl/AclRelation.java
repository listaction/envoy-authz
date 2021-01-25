package authserver.acl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclRelation {

    private String object;
    private String relation;
    private Set<AclRelationParent> parents;

}
