package org.example.authserver.entity;

import authserver.acl.AclRelationConfig;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "configs")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AclRelationConfigEntity {

  @Id private String id;
  private String namespace;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private AclRelationConfig config;

  public static AclRelationConfig toAclRelationConfig(AclRelationConfigEntity entity) {
    return entity.getConfig();
  }
}
