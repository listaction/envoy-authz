package org.example.authserver.repo.filter;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import org.example.authserver.entity.AclEntity;

public class AclFilter {

  public static String getSortableField(String field) {
    switch (field) {
      case "id":
        return "id";
      case "namespace":
        return "namespace";
      case "object":
        return "object";
      case "relation":
        return "relation";
      case "user":
        return "user";
      case "created":
        return "created";
      case "updated":
        return "updated";
      default:
    }

    return null;
  }

  public static void applyFilterForUsers(
      Map<String, String> filterParams, Filter<AclEntity> filter) {
    for (Map.Entry<String, String> entry : filterParams.entrySet()) {
      switch (entry.getKey().toLowerCase()) {
        case "id":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("id")
                  .value(entry.getValue())
                  .type(FilterFieldTypes.STRING)
                  .build());
          break;

        case "namespace":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("namespace")
                  .value(entry.getValue())
                  .type(FilterFieldTypes.STRING)
                  .build());
          break;

        case "object":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("object")
                  .value(entry.getValue())
                  .type(FilterFieldTypes.STRING)
                  .build());
          break;

        case "nsobject":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("nsobject")
                  .value(entry.getValue())
                  .type(FilterFieldTypes.STRING)
                  .build());
          break;

        case "relation":
          if (entry.getValue().contains(",")) {
            final String[] relations =
                Arrays.stream(entry.getValue().split(",")).toArray(String[]::new);

            filter.addCondition(
                Condition.builder()
                    .comparison(FilterComparison.EQ)
                    .field("relation")
                    .value(relations)
                    .type(FilterFieldTypes.STRING)
                    .build());
          } else {
            filter.addCondition(
                Condition.builder()
                    .comparison(FilterComparison.EQ)
                    .field("relation")
                    .value(entry.getValue())
                    .type(FilterFieldTypes.STRING)
                    .build());
          }
          break;

        case "user":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("user")
                  .value(entry.getValue())
                  .type(FilterFieldTypes.STRING)
                  .build());
          break;

        case "userset_namespace":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("usersetNamespace")
                  .value(entry.getValue())
                  .type(FilterFieldTypes.STRING)
                  .build());
          break;

        case "userset_object":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("usersetObject")
                  .value(entry.getValue())
                  .type(FilterFieldTypes.STRING)
                  .build());
          break;

        case "userset_relation":
          if (entry.getValue().contains(",")) {
            final String[] usersetRelations =
                Arrays.stream(entry.getValue().split(",")).toArray(String[]::new);

            filter.addCondition(
                Condition.builder()
                    .comparison(FilterComparison.EQ)
                    .field("usersetRelation")
                    .value(usersetRelations)
                    .type(FilterFieldTypes.STRING)
                    .build());
          } else {
            filter.addCondition(
                Condition.builder()
                    .comparison(FilterComparison.EQ)
                    .field("usersetRelation")
                    .value(entry.getValue())
                    .type(FilterFieldTypes.STRING)
                    .build());
          }
          break;

        case "created_eq":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("created")
                  .value(Date.from(Instant.parse(entry.getValue())))
                  .type(FilterFieldTypes.DATE)
                  .build());
          break;

        case "created_gte":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.GTE)
                  .field("created")
                  .value(Date.from(Instant.parse(entry.getValue())))
                  .type(FilterFieldTypes.DATE)
                  .build());
          break;

        case "created_lte":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.LTE)
                  .field("created")
                  .value(Date.from(Instant.parse(entry.getValue())))
                  .type(FilterFieldTypes.DATE)
                  .build());
          break;

        case "updated_eq":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.EQ)
                  .field("updated")
                  .value(Date.from(Instant.parse(entry.getValue())))
                  .type(FilterFieldTypes.DATE)
                  .build());
          break;

        case "updated_gte":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.GTE)
                  .field("updated")
                  .value(Date.from(Instant.parse(entry.getValue())))
                  .type(FilterFieldTypes.DATE)
                  .build());
          break;

        case "updated_lte":
          filter.addCondition(
              Condition.builder()
                  .comparison(FilterComparison.LTE)
                  .field("invitupdatedeAcceptedDate")
                  .value(Date.from(Instant.parse(entry.getValue())))
                  .type(FilterFieldTypes.DATE)
                  .build());
          break;

        default:
      }
    }
  }
}
