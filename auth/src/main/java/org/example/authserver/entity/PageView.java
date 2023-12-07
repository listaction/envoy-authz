package org.example.authserver.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

@SuperBuilder
@Data
@NoArgsConstructor
public class PageView<T> {
  private long total;
  private long pages;
  private long currentPage;
  private Collection<T> data;

  public static long calculateTotalPages(long totalRecords, long pageSize) {
    return (totalRecords + pageSize - 1) / pageSize;
  }
}