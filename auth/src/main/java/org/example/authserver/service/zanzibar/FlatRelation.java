package org.example.authserver.service.zanzibar;

import lombok.Getter;

@Getter
public class FlatRelation {
  private final String namespace;
  private final String object;
  private final String relation;
  private final String parent;

  public FlatRelation(String namespace, String object, String relation, String parent) {
    this.namespace = namespace;
    this.object = object;
    this.relation = relation;
    this.parent = parent;
  }

  public FlatRelation(String namespaceAndObject, String relation, String parent) {
    this(parseNamespace(namespaceAndObject), parseObject(namespaceAndObject), relation, parent);
  }

  private static String parseNamespace(String namespaceAndObject) {
    String[] tmp = namespaceAndObject.split(":");
    return tmp[0];
  }

  private static String parseObject(String namespaceAndObject) {
    String[] tmp = namespaceAndObject.split(":");
    return tmp[1];
  }

  @Override
  public String toString() {
    return String.format("%s:%s#%s", namespace, object, relation);
  }
}
