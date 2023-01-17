package org.example.authserver.entity;

import java.util.*;

public class MappingDTO {

  private final Map<String, String> variables = new HashMap<>();
  private final MappingEntity mappingEntity;

  public MappingDTO(MappingEntity mappingEntity) {
    this.mappingEntity = mappingEntity;
  }

  public String getVariable(String key) {
    return variables.get(key);
  }

  public Map<String, String> getVariableMap() {
    return variables;
  }

  public List<String> getRoles() {
    return Optional.ofNullable(mappingEntity.getRoles()).orElse(new ArrayList<>());
  }

  public List<String> getGroupRoles() {
    return Optional.ofNullable(mappingEntity.getGroupRoles()).orElse(new ArrayList<>());
  }

  public MappingEntity getMappingEntity() {
    return mappingEntity;
  }
}
