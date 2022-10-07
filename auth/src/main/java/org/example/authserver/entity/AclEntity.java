package org.example.authserver.entity;

import authserver.acl.Acl;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Log
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "acls")
public class AclEntity implements Serializable {

  @Id private String id;
  private String nsobject;

  @Column(name = "usr")
  private String user;

  private String relation;

  private String namespace;
  private String object;

  private String usersetNamespace;
  private String usersetObject;
  private String usersetRelation;

  @Builder.Default private Long created = System.currentTimeMillis();
  @Builder.Default private Long updated = System.currentTimeMillis();

  public Acl toAcl() {
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
