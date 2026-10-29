package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.interceptor.test.ScheduledRecords;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
@Varied
public class VariedScheduledService {

    static final String RAN = "VariedScheduledService";

    private final ScheduledRecords records;

    public VariedScheduledService(ScheduledRecords records) {
        this.records = records;
    }

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        records.record(VariedCalls.LOG, "scheduled");
        records.ran(RAN);
    }
}
