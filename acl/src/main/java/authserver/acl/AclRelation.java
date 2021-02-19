package authserver.acl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclRelation {

    private String object;
    private String relation;
    @Builder.Default
    private Set<AclRelationParent> parents = new HashSet<>();
    @Builder.Default
    private Set<String> exclusions = new HashSet<>();
    @Builder.Default
    private Set<String> intersections = new HashSet<>();

}
