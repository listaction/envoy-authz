package authserver.acl;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;

@Data
public class TreeNode<T> implements Iterable<TreeNode<T>> {

  private T data;
  private TreeNode<T> parent;
  private List<TreeNode<T>> children;
  private long id;
  private long left;
  private long right;

  public boolean isRoot() {
    return parent == null;
  }

  public boolean isLeaf() {
    return children.size() == 0;
  }

  private List<TreeNode<T>> elementsIndex;

  public TreeNode(T data) {
    this.data = data;
    this.children = new LinkedList<>();
    this.elementsIndex = new LinkedList<>();
    this.elementsIndex.add(this);
  }

  public TreeNode<T> addChild(T child) {
    TreeNode<T> childNode = new TreeNode<T>(child);
    childNode.parent = this;
    this.children.add(childNode);
    this.registerChildForSearch(childNode);
    return childNode;
  }

  public int getLevel() {
    if (this.isRoot()) return 0;
    else return parent.getLevel() + 1;
  }

  private void registerChildForSearch(TreeNode<T> node) {
    elementsIndex.add(node);
    if (parent != null) parent.registerChildForSearch(node);
  }

  public TreeNode<T> findTreeNode(Comparable<T> cmp) {
    for (TreeNode<T> element : this.elementsIndex) {
      T elData = element.data;
      if (cmp.compareTo(elData) == 0) return element;
    }

    return null;
  }

  @Override
  public String toString() {
    return "TreeNode{data=" + data + '}';
  }

  public String treeAsString() {
    StringBuilder sb = new StringBuilder();
    sb.append("-".repeat(Math.max(0, getLevel())));
    sb.append(data != null ? data.toString() : "[data null]");
    sb.append(String.format(" [%s;%s]", left, right));
    sb.append("\n");
    for (TreeNode<T> child : getChildren()) {
      sb.append(child.toString());
    }
    return sb.toString();
  }

  @Override
  public Iterator<TreeNode<T>> iterator() {
    return new TreeNodeIter<T>(this);
  }
}
