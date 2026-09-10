package io.micronaut.interceptor.test.timeout;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Records the thread its scheduled method runs on. Only the first run is kept, for the interceptor and the method
 * alike, so that the two threads compared are those of one run of the schedule.
 */
@Singleton
@Threaded
public class ThreadedScheduledService {

    static final AtomicReference<Thread> INTERCEPTOR_THREAD = new AtomicReference<>();
    static final AtomicReference<Thread> METHOD_THREAD = new AtomicReference<>();
    static final CountDownLatch RAN = new CountDownLatch(1);

    @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
    public void onSchedule() {
        if (METHOD_THREAD.compareAndSet(null, Thread.currentThread())) {
            RAN.countDown();
        }
    }
}
