package org.example.authserver.entity;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class MappingEntityList {
  @NotNull private List<MappingEntity> mappings;
}
