package org.example.authserver.service.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.authserver.entity.MappingEntity;

public class Mapping {

  private final Map<String, String> variables = new HashMap<>();
  private final MappingEntity mappingEntity;

  public Mapping(MappingEntity mappingEntity) {
    this.mappingEntity = mappingEntity;
  }

  public String getVariable(String key) {
    return variables.get(key);
  }

  public Map<String, String> getVariableMap() {
    return variables;
  }

  public List<String> getRoles() {
    return mappingEntity.getRoles();
  }

  public MappingEntity getMappingEntity() {
    return mappingEntity;
  }
}
