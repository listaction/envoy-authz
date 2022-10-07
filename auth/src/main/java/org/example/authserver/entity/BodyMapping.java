package org.example.authserver.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BodyMapping {

  private BodyMappingTypes type;
  private List<BodyMappingKey> keys;
}
