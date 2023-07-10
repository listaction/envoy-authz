package org.example.authserver.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "rel_cache")
public class RelCache {

  @Id private String id;
  private String nsobject;
  private String relation;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", name = "nested_relations")
  private Set<String> nestedRelations;

  private String path;
  private String usr;
  private long rev;
}
