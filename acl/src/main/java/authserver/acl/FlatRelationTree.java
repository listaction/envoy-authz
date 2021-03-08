package authserver.acl;

import lombok.Data;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlatRelationTree)) return false;
        FlatRelationTree that = (FlatRelationTree) o;
        return namespace.equals(that.namespace) && object.equals(that.object) && relation.equals(that.relation) && left.equals(that.left) && right.equals(that.right) && level.equals(that.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left);
    }
}
