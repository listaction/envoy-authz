package org.example.authserver.entity;

import authserver.acl.AclRelationConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "configs")
public class AclRelationConfigEntity {

  @Id private String id;
  private String namespace;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private AclRelationConfig config;

  public static AclRelationConfig toAclRelationConfig(AclRelationConfigEntity entity) {
    return entity.getConfig();
  }
}
