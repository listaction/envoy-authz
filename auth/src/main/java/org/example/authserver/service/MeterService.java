package org.example.authserver.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MeterService {

    private static final String REQUESTS_HITS_CACHE = "hits_cache";
    private static final String REQUESTS_HITS_ZANZIBAR = "hits_zanzibar";

    private static final Map<String, Counter> counters = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;

    public MeterService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        init();
    }

    private void init() {
        counters.put(
                REQUESTS_HITS_CACHE,
                Counter.builder(REQUESTS_HITS_CACHE)
                        .register(meterRegistry)
        );
        counters.put(
                REQUESTS_HITS_ZANZIBAR,
                Counter.builder(REQUESTS_HITS_ZANZIBAR)
                        .register(meterRegistry)
        );
    }

    public void countHitsCache() {
        counters.get(REQUESTS_HITS_CACHE).increment();
    }

    public void countHitsZanzibar() {
        counters.get(REQUESTS_HITS_ZANZIBAR).increment();
    }

}
