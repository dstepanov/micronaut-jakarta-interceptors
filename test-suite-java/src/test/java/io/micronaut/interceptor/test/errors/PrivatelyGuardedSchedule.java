package io.micronaut.interceptor.test.errors;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

/**
 * A timeout method, which the test invokes itself: the scheduler is not due to for as long as the test runs.
 */
@Singleton
@PrivatelyGuarded
public class PrivatelyGuardedSchedule {

    @Scheduled(fixedDelay = "1h", initialDelay = "1h")
    public void onSchedule() {
    }
}
