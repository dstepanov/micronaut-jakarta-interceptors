package io.micronaut.interceptor.test.timeout;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.util.concurrent.CountDownLatch;

@Singleton
@Timed
@Watched
public class ScheduledService {

    static final CountDownLatch SCHEDULED_RAN = new CountDownLatch(1);

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        SCHEDULED_RAN.countDown();
    }

    public String business() {
        return "business";
    }
}
