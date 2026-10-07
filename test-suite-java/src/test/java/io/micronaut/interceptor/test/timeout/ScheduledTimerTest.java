package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.ApplicationContext;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code getTimer()} stands for the timer that invoked the timeout method. A method with two schedules
 * is invoked by each of them, and each invocation should show its own; a direct call by the application is a
 * business method invocation, interposed on by {@code @AroundInvoke}.
 */
class ScheduledTimerTest {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();
    static volatile CountDownLatch bothRan = new CountDownLatch(2);
    // counted down by the direct call made before the schedule runs, and by the schedule
    static volatile CountDownLatch laterRan = new CountDownLatch(2);

    @Test
    void eachScheduleShowsItsOwnTimer() throws Exception {
        CALLS.clear();
        bothRan = new CountDownLatch(2);
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(bothRan.await(5, TimeUnit.SECONDS), "both schedules ran");

            Set<String> timers = new TreeSet<>();
            for (String call : CALLS) {
                if (call.startsWith("timeout TwiceScheduledService ")) {
                    timers.add(call.substring("timeout TwiceScheduledService ".length()));
                }
            }
            assertEquals(2, timers.size(), "one timer per schedule: " + CALLS);
        }
    }

    @Test
    void aDirectCallIsABusinessMethodInvocation() {
        try (ApplicationContext context = ApplicationContext.run()) {
            CALLS.clear();
            context.getBean(DirectlyCalledService.class).onSchedule();

            assertEquals(List.of("invoke DirectlyCalledService"),
                CALLS.stream().filter(call -> call.contains("DirectlyCalledService")).toList());
        }
    }

    /**
     * The same method of the same object, called directly before and after the scheduler invokes it: each direct
     * call is a business method invocation and the scheduled one a timeout, whichever came first.
     */
    @Test
    void keepsTheTimeoutAndTheBusinessInvocationsOfOneMethodApart() throws Exception {
        CALLS.clear();
        laterRan = new CountDownLatch(2);
        try (ApplicationContext context = ApplicationContext.run()) {
            LaterScheduledService service = context.getBean(LaterScheduledService.class);
            service.onSchedule();
            assertTrue(laterRan.await(5, TimeUnit.SECONDS), "the schedule ran");
            service.onSchedule();

            List<String> calls = CALLS.stream()
                .filter(call -> call.contains("LaterScheduledService"))
                .map(call -> call.startsWith("timeout") ? "timeout" : call)
                .toList();
            // the schedule normally runs between the two direct calls, but a slow start may let it run first:
            // either way each call is interposed on as what it is, and the last direct call is a business one
            assertEquals(List.of("invoke LaterScheduledService", "invoke LaterScheduledService", "timeout"),
                calls.stream().sorted().toList());
            assertEquals("invoke LaterScheduledService", calls.get(calls.size() - 1));
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface TwiceTimed {
    }

    @Interceptor
    @TwiceTimed
    public static class TwiceTimedInterceptor {

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            CALLS.add("invoke " + context.getMethod().getDeclaringClass().getSimpleName());
            return context.proceed();
        }

        @AroundTimeout
        public Object timeout(InvocationContext context) throws Exception {
            CALLS.add("timeout " + context.getMethod().getDeclaringClass().getSimpleName() + " " + context.getTimer());
            return context.proceed();
        }
    }

    @Singleton
    @TwiceTimed
    public static class TwiceScheduledService {

        @Scheduled(fixedDelay = "1h", initialDelay = "10ms")
        @Scheduled(fixedDelay = "2h", initialDelay = "50ms")
        public void onSchedule() {
            bothRan.countDown();
        }
    }

    @Singleton
    @TwiceTimed
    public static class DirectlyCalledService {

        @Scheduled(fixedDelay = "1h", initialDelay = "1h")
        public void onSchedule() {
        }
    }

    @Singleton
    @TwiceTimed
    public static class LaterScheduledService {

        @Scheduled(fixedDelay = "1h", initialDelay = "1s")
        public void onSchedule() {
            laterRan.countDown();
        }
    }
}
