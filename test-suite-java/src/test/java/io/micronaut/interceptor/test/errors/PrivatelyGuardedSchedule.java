package io.micronaut.interceptor.test.errors;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

/**
 * A timeout method, which the test invokes itself: the scheduler is not due to for as long as the test runs.
 */
@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
@PrivatelyGuarded
public class PrivatelyGuardedSchedule {

    @Scheduled(fixedDelay = "1h", initialDelay = "1h")
    public void onSchedule() {
    }
}
