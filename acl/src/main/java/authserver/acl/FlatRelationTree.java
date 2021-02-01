package authserver.acl;

import lombok.Data;

@Data
public class FlatRelationTree {
    private final String namespace;
    private final String object;
    private final String relation;
    private final Long left;
    private final Long right;
    private final Integer level;

    public FlatRelationTree(String namespace, String object, String relation, Long left, Long right, Integer level) {
        this.namespace = namespace;
        this.object = object;
        this.relation = relation;
        this.left = left;
        this.right = right;
        this.level = level;
    }

    @Override
    public String toString() {
        return String.format("%s:%s#%s [%s;%s]{%s}", namespace, object, relation, left, right, level);
    }

}
