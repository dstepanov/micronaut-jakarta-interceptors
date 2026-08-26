package io.micronaut.interceptor.test.timeout;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

import java.util.concurrent.CountDownLatch;

@Singleton
@Interceptors(NamedTimeoutInterceptor.class)
public class NamedScheduledService {

    static final CountDownLatch RAN = new CountDownLatch(1);

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        RAN.countDown();
    }
}
