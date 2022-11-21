package com.example.splittest.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeterService {
  private static final String AUTHZ_OK = "authz_ok";
  private static final String AUTHZ_MISMATCH = "authz_mismatch";

  private static final String ACLS_CREATED = "acls_created";

  private static final String ACLS_DELETED = "acls_deleted";

  private static final Map<String, Counter> counters = new ConcurrentHashMap<>();
  private final MeterRegistry meterRegistry;

  public MeterService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    init();
  }

  private void init() {
    counters.put(AUTHZ_OK, Counter.builder(AUTHZ_OK).register(meterRegistry));
    counters.put(AUTHZ_MISMATCH, Counter.builder(AUTHZ_MISMATCH).register(meterRegistry));
  }

  public void countAuthzOk() {
    counters.get(AUTHZ_OK).increment();
  }

  public void countAuthzMismatch() {
    counters.get(AUTHZ_MISMATCH).increment();
  }

  public void countAclsCreated() {
    counters.get(ACLS_CREATED).increment();
  }

  public void countAclsDeleted() {
    counters.get(ACLS_DELETED).increment();
  }
}
