package org.example.authserver.repo.filter;

import static jakarta.persistence.criteria.Predicate.BooleanOperator.AND;

import jakarta.persistence.criteria.Predicate.BooleanOperator;
import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Condition<T> {

  private FilterFieldTypes type;
  private FilterComparison comparison;
  private String field;
  private T value;
  @Builder.Default private BooleanOperator operator = AND;

  public Number getValueAsNumber() {
    if (value instanceof Date) {
      return ((Date) value).getTime();
    } else if (value instanceof Integer) {
      return (Integer) value;
    } else if (value instanceof Long) {
      return (Long) value;
    } else if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    }
    return 0;
  }
}
