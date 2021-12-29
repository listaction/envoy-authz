package org.example.authserver.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "rel_cache")
public class RelCache {

  @Id
  private String id;
  private String nsobject;
  private String relation;
  private String path;
  private String usr;
  private long rev;

}
