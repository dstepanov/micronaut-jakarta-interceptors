package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.inject.Singleton;

/** A bean an around-timeout interceptor is given, and calls as it interposes. */
@Singleton
public class TimeoutCollaborator {

    private final ScheduledRecords records;

    public TimeoutCollaborator(ScheduledRecords records) {
        this.records = records;
    }

    public void consulted() {
        records.record(VariedCalls.LOG, "collaborator");
    }
}
