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
public class AclRelationConfig {

    private String namespace;
    @Builder.Default
    private Set<AclRelation> relations = new HashSet<>();

}
