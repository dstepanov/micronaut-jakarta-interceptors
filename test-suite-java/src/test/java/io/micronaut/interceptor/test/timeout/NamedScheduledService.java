package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.interceptor.test.ScheduledRecords;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
@Interceptors(NamedTimeoutInterceptor.class)
public class NamedScheduledService {

    static final String RAN = "NamedScheduledService";

    private final ScheduledRecords records;

    public NamedScheduledService(ScheduledRecords records) {
        this.records = records;
    }

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        records.ran(RAN);
    }
}
