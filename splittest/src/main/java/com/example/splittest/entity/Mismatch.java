package com.example.splittest.entity;

import authserver.common.CheckTestDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "mismatches")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class Mismatch {

  @Id private Long id;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private CheckTestDto checkTestDto;

  @Column(columnDefinition = "text")
  private String requestPath;

  private String requestMethod;

  @Column(columnDefinition = "text")
  private String expectedTags;

  @Column(columnDefinition = "text")
  private String actualTags;

  private String tenant;
  private String userId;

  @Column(columnDefinition = "text")
  private String debug;

  @Column(columnDefinition = "text")
  private String grpcResponse;

  private Boolean expected;
  private Boolean actual;
  private Boolean actualUpdated;
  private Boolean tagsMismatch;
  private Boolean resultMismatch;

  private Date created;
  private Date updated;

  private Integer attempts;
}
