package org.example.authserver.repo.filter;

import jakarta.persistence.criteria.*;
import jakarta.persistence.criteria.Predicate.BooleanOperator;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;

public class Filter<T> implements Specification<T> {

  private final List<Condition> conditions = new ArrayList<>();

  public void addCondition(Condition condition) {
    this.conditions.add(condition);
  }

  private List<Predicate> buildPredicates(Root root, CriteriaQuery query, CriteriaBuilder cb) {
    List<Predicate> predicates = new ArrayList<>();
    for (Condition condition : conditions) {
      predicates.add(buildPredicate(condition, root, query, cb));
    }
    return predicates;
  }

  public Predicate buildPredicate(
      Condition condition, Root root, CriteriaQuery query, CriteriaBuilder cb) {
    if (!condition.getType().getValue().contains(condition.getComparison())) {
      throw new RuntimeException("UNSUPPORTED_COMPARISON_FOR_SPECIFIED_TYPE");
    }
    switch (condition.getComparison()) {
      case EQ:
        if (condition.getValue().getClass().isArray()) {
          final Predicate[] predicatesList =
              Arrays.stream((Object[]) condition.getValue())
                  .map(conditionValue -> cb.equal(root.get(condition.getField()), conditionValue))
                  .toArray(Predicate[]::new);
          return cb.or(predicatesList);
        }
        return cb.equal(root.get(condition.getField()), condition.getValue());
      case GT:
        if (condition.getType().equals(FilterFieldTypes.DATE)) {
          return cb.greaterThan(
              root.<Date>get(condition.getField()),
              new Date(condition.getValueAsNumber().longValue()));
        } else {
          return cb.gt(root.get(condition.getField()), condition.getValueAsNumber());
        }
      case GTE:
        if (condition.getType().equals(FilterFieldTypes.DATE)) {
          return cb.greaterThanOrEqualTo(
              root.<Date>get(condition.getField()),
              new Date(condition.getValueAsNumber().longValue()));
        } else {
          return cb.ge(root.get(condition.getField()), condition.getValueAsNumber());
        }
      case LT:
        if (condition.getType().equals(FilterFieldTypes.DATE)) {
          return cb.lessThan(
              root.<Date>get(condition.getField()),
              new Date(condition.getValueAsNumber().longValue()));
        } else {
          return cb.lt(root.get(condition.getField()), condition.getValueAsNumber());
        }
      case LTE:
        if (condition.getType().equals(FilterFieldTypes.DATE)) {
          return cb.lessThanOrEqualTo(
              root.<Date>get(condition.getField()),
              new Date(condition.getValueAsNumber().longValue()));
        } else {
          return cb.le(root.get(condition.getField()), condition.getValueAsNumber());
        }
      case EXISTS:
        if (condition.getValue() == null || Boolean.parseBoolean(condition.getValue().toString())) {
          return cb.isNotNull(root.get(condition.getField()));
        } else {
          return cb.isNull(root.get(condition.getField()));
        }
      default:
        throw new RuntimeException("UNSUPPORTED_COMPARISON");
    }
  }

  public int getConditionsCount() {
    return conditions.size();
  }

  @Override
  public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
    List<Predicate> predicates = buildPredicates(root, query, cb);
    if (predicates.size() == 1) {
      return predicates.get(0);
    }
    boolean allOrs =
        conditions.stream()
            .allMatch(condition -> condition.getOperator().equals(BooleanOperator.OR));
    return allOrs
        ? cb.or(predicates.toArray(Predicate[]::new))
        : cb.and(predicates.toArray(Predicate[]::new));
  }
}
