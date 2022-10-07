package org.example.authserver.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.http.HttpMethod;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "mappings")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class MappingEntity implements Serializable {

  @Id private String id;

  @Enumerated(EnumType.STRING)
  private HttpMethod method;

  private String path;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<HeaderMappingKey> headerMapping;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private BodyMapping bodyMapping;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private List<String> roles;

  private String namespace;
  private String object;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MappingEntity)) return false;
    MappingEntity that = (MappingEntity) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
