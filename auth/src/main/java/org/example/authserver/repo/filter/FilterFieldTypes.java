package org.example.authserver.repo.filter;

import static org.example.authserver.repo.filter.FilterComparison.*;

import java.util.List;

public enum FilterFieldTypes {
  STRING(List.of(EQ, EXISTS)),
  ENUM(List.of(EQ)),
  DATE(List.of(EQ, LT, GT, LTE, GTE)),
  NUMBER(List.of(EQ, LT, GT, LTE, GTE)),
  BOOLEAN(List.of(EQ));

  FilterFieldTypes(List<FilterComparison> value) {
    this.value = value;
  }

  private final List<FilterComparison> value;

  public List<FilterComparison> getValue() {
    return value;
  }
}
