package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.interceptor.test.ScheduledRecords;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

/**
 * Names its timeout interceptor on the method rather than on the class, and schedules a second method that names
 * none: what the specification associates with a method is the method's alone.
 */
@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
public class MethodNamedScheduledService {

    static final String NAMED_RAN = "MethodNamedScheduledService.named";
    static final String PLAIN_RAN = "MethodNamedScheduledService.plain";

    private final ScheduledRecords records;

    public MethodNamedScheduledService(ScheduledRecords records) {
        this.records = records;
    }

    @Interceptors(MethodNamedTimeoutInterceptor.class)
    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void named() {
        records.ran(NAMED_RAN);
    }

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void plain() {
        records.ran(PLAIN_RAN);
    }
}
