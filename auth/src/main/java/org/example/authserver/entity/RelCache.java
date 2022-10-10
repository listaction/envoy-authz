package org.example.authserver.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "rel_cache")
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
public class RelCache {

  @Id private String id;
  private String nsobject;
  private String relation;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb", name = "nested_relations")
  private Set<String> nestedRelations;

  private String path;
  private String usr;
  private long rev;
}
