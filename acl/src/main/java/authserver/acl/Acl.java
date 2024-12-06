package authserver.acl;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.json.JSONObject;

@Log
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Acl implements Cloneable, Serializable {

  private static final Pattern aclExprPattern = Pattern.compile("(\\S+):(\\S+)#(\\S+)@(\\S+)");
  private static final Pattern usersetPattern = Pattern.compile("(\\S+):(\\S+)#(\\S+)");

  @Builder.Default private UUID id = UUID.randomUUID();
  private String namespace;
  private String object;
  private String relation;
  private String user;

  private String usersetNamespace;
  private String usersetObject;
  private String usersetRelation;

  @Builder.Default private Long created = System.currentTimeMillis();
  @Builder.Default private Long updated = System.currentTimeMillis();

  public static Acl create(String aclExpression) {
    return parseAcl(aclExpression);
  }

  public boolean hasUserset() {
    return isNotEmpty(usersetNamespace) && isNotEmpty(usersetObject) && isNotEmpty(usersetRelation);
  }

  public String getNsObject() {
    return String.format("%s:%s", namespace, object);
  }

  public String getTag() {
    return String.format("%s:%s#%s", namespace, object, relation);
  }

  public static Set<String> getTags(Set<Acl> acls) {
    return acls.stream().map(Acl::getTag).collect(Collectors.toSet());
  }

  private boolean isNotEmpty(String s) {
    if (s == null) return false;
    if (s.length() == 0) return false;
    return true;
  }

  public Acl clone() {
    try {
      return (Acl) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Acl)) return false;
    Acl acl = (Acl) o;
    return namespace.equals(acl.namespace)
        && object.equals(acl.object)
        && relation.equals(acl.relation)
        && Objects.equals(user, acl.user)
        && Objects.equals(usersetNamespace, acl.usersetNamespace)
        && Objects.equals(usersetObject, acl.usersetObject)
        && Objects.equals(usersetRelation, acl.usersetRelation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        namespace, object, relation, user, usersetNamespace, usersetObject, usersetRelation);
  }

  @Override
  public String toString() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", id.toString());
    jsonObject.put("namespace", namespace);
    jsonObject.put("object", object);
    jsonObject.put("relation", relation);
    jsonObject.put("user", user);
    jsonObject.put("usersetNamespace", usersetNamespace);
    jsonObject.put("usersetObject", usersetObject);
    jsonObject.put("usersetRelation", usersetRelation);
    jsonObject.put("created", created);
    jsonObject.put("updated", updated);

    return jsonObject.toString();
  }

  private static Acl parseAcl(String aclExpr) {
    Matcher m = aclExprPattern.matcher(aclExpr);
    if (!m.find()) {
      log.log(Level.WARNING, String.format("Can't parse ACL: %s", aclExpr));
      return null;
    }

    String namespace = m.group(1);
    String object = m.group(2);
    String relation = m.group(3);
    String user = m.group(4);

    String usersetNamespace = null;
    String usersetObject = null;
    String usersetRelation = null;

    log.log(Level.FINE, String.format("acl: %s", aclExpr));
    log.log(
        Level.FINE,
        String.format(
            "acl -> namespace: %s , object: %s, relation: %s, user: %s",
            namespace, object, relation, user));

    m = usersetPattern.matcher(m.group(4));
    if (m.find()) {
      usersetNamespace = m.group(1);
      usersetObject = m.group(2);
      usersetRelation = m.group(3);

      log.log(
          Level.FINE,
          String.format(
              "userset -> namespace: %s, object: %s, relation: %s",
              usersetNamespace, usersetObject, usersetRelation));
    }

    return Acl.builder()
        .namespace(namespace)
        .object(object)
        .relation(relation)
        .user(user)
        .usersetNamespace(usersetNamespace)
        .usersetObject(usersetObject)
        .usersetRelation(usersetRelation)
        .build();
  }
}
