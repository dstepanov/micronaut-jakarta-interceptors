package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.interceptor.test.ScheduledRecords;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.8 bb): an around-timeout method may be declared on the target class itself.
 */
@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
public class SelfTimeoutService {

    static final String RAN = "SelfTimeoutService";
    static final String INTERCEPTED = "SelfTimeoutService.intercepted";

    private final ScheduledRecords records;

    public SelfTimeoutService(ScheduledRecords records) {
        this.records = records;
    }

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        records.ran(RAN);
    }

    @AroundTimeout
    Object aroundItsOwnTimeout(InvocationContext context) throws Exception {
        records.record(INTERCEPTED, context.getMethod().getName());
        return context.proceed();
    }
}
