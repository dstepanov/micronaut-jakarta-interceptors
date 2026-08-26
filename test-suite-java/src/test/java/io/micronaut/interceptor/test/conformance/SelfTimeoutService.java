package io.micronaut.interceptor.test.conformance;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

import java.util.concurrent.CountDownLatch;

/**
 * Section 2.8 bb): an around-timeout method may be declared on the target class itself.
 */
@Singleton
public class SelfTimeoutService {

    static final CountDownLatch RAN = new CountDownLatch(1);
    static volatile boolean intercepted;

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        RAN.countDown();
    }

    @AroundTimeout
    Object aroundItsOwnTimeout(InvocationContext context) throws Exception {
        intercepted = true;
        return context.proceed();
    }
}
