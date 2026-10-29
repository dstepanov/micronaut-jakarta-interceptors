package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.annotation.Requires;
import io.micronaut.interceptor.test.ScheduledFixtures;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.util.concurrent.CountDownLatch;

@Singleton
@Requires(property = ScheduledFixtures.ENABLED, value = "true")
@Varied
public class VariedScheduledService {

    static final CountDownLatch RAN = new CountDownLatch(1);

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        VariedCalls.RECORDED.add("scheduled");
        RAN.countDown();
    }
}
