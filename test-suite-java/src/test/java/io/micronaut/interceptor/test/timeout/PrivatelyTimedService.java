package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.interceptor.test.ScheduledRecords;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
@PrivatelyTimed
public class PrivatelyTimedService {

    static final String RAN = "PrivatelyTimedService";

    private final ScheduledRecords records;

    public PrivatelyTimedService(ScheduledRecords records) {
        this.records = records;
    }

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        records.ran(RAN);
    }
}
