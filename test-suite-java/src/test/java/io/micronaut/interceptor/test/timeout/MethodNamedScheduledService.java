package io.micronaut.interceptor.test.timeout;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

import java.util.concurrent.CountDownLatch;

/**
 * Names its timeout interceptor on the method rather than on the class, and schedules a second method that names
 * none: what the specification associates with a method is the method's alone.
 */
@Singleton
public class MethodNamedScheduledService {

    static final CountDownLatch NAMED_RAN = new CountDownLatch(1);
    static final CountDownLatch PLAIN_RAN = new CountDownLatch(1);

    @Interceptors(MethodNamedTimeoutInterceptor.class)
    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void named() {
        NAMED_RAN.countDown();
    }

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void plain() {
        PLAIN_RAN.countDown();
    }
}
