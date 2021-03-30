package org.example.authserver.entity;

import authserver.acl.AclRelation;
import authserver.acl.AclRelationConfig;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.authserver.Utils;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "configs")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AclRelationConfigEntity {

    @Id
    private String id;
    private String namespace;
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private AclRelationConfig config;


    public static AclRelationConfig toAclRelationConfig(AclRelationConfigEntity entity) {
        return entity.getConfig();
    }

}
