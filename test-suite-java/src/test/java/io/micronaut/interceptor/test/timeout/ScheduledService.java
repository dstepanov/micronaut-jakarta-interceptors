package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.interceptor.test.ScheduledRecords;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
@Timed
@Watched
public class ScheduledService {

    static final String RAN = "ScheduledService";

    private final ScheduledRecords records;

    public ScheduledService(ScheduledRecords records) {
        this.records = records;
    }

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        records.ran(RAN);
    }

    public String business() {
        return "business";
    }
}
