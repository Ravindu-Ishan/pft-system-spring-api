package org.spring.pftsystem.services;


import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class SystemUsageTracker {

    private final MeterRegistry meterRegistry;

    public SystemUsageTracker(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public long getTotalRequestCount() {
        try {
            // This gets the total count of all HTTP requests
            return (long) meterRegistry.get("http.server.requests").counter().count();
        } catch (Exception e) {
            // If the metric doesn't exist or there's another issue, return 0
            return 0;
        }
    }
}