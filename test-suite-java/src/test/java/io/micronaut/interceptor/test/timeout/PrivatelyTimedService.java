package io.micronaut.interceptor.test.timeout;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.util.concurrent.CountDownLatch;

@Singleton
@PrivatelyTimed
public class PrivatelyTimedService {

    static final CountDownLatch RAN = new CountDownLatch(1);

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        RAN.countDown();
    }
}
