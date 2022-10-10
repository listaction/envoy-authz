package org.example.authserver.entity;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MappingEntityList {
  @NotNull private List<MappingEntity> mappings;
}
