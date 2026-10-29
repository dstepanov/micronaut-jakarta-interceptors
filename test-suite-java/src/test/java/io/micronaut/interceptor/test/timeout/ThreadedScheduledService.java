package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.interceptor.test.ScheduledRecords;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

/**
 * Records the thread its scheduled method runs on. Only the first run is kept, for the interceptor and the method
 * alike, so that the two threads compared are those of one run of the schedule.
 */
@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
@Threaded
public class ThreadedScheduledService {

    static final String INTERCEPTOR_THREAD = "ThreadedScheduledService.interceptor";
    static final String METHOD_THREAD = "ThreadedScheduledService.method";
    static final String RAN = "ThreadedScheduledService";

    private final ScheduledRecords records;

    public ThreadedScheduledService(ScheduledRecords records) {
        this.records = records;
    }

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        if (records.firstThread(METHOD_THREAD)) {
            records.ran(RAN);
        }
    }
}
