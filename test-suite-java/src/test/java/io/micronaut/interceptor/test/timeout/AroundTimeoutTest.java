package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A method the Micronaut scheduler invokes is what stands here for the timeout method of the specification.
 */
class AroundTimeoutTest {

    @Test
    void interposesOnAScheduledMethodAndOnABusinessMethodApart() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(ScheduledService.SCHEDULED_RAN.await(5, TimeUnit.SECONDS), "the schedule did not run");

            assertEquals("business", context.getBean(ScheduledService.class).business());

            List<String> timedCalls = List.copyOf(TimedInterceptor.CALLS);
            assertTrue(timedCalls.contains("aroundInvoke business"),
                "a business method is interposed on by @AroundInvoke: " + timedCalls);
            assertTrue(timedCalls.stream().noneMatch(call -> call.equals("aroundInvoke onSchedule")),
                "a scheduled method is not interposed on by @AroundInvoke: " + timedCalls);
            assertTrue(timedCalls.stream().anyMatch(call -> call.startsWith("aroundTimeout onSchedule")),
                "a scheduled method is interposed on by @AroundTimeout: " + timedCalls);
        }
    }

    @Test
    void showsTheScheduleAsTheTimer() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(ScheduledService.SCHEDULED_RAN.await(5, TimeUnit.SECONDS), "the schedule did not run");

            String call = TimedInterceptor.CALLS.stream()
                .filter(it -> it.startsWith("aroundTimeout onSchedule"))
                .findFirst()
                .orElseThrow();
            assertTrue(call.contains("fixedDelay=20ms"), call);
            assertTrue(call.contains("initialDelay=10ms"), call);
        }
    }

    @Test
    void interposesOnAScheduledMethodThroughAnInterceptorNamedDirectly() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(NamedScheduledService.RAN.await(5, TimeUnit.SECONDS), "the schedule did not run");

            List<String> calls = List.copyOf(NamedTimeoutInterceptor.CALLS);
            assertTrue(calls.contains("named aroundTimeout onSchedule"), calls.toString());
        }
    }

    @Test
    void fallsBackToAroundInvokeWhenNoTimeoutMethodIsDeclared() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(ScheduledService.SCHEDULED_RAN.await(5, TimeUnit.SECONDS), "the schedule did not run");

            List<String> calls = List.copyOf(OnlyAroundInvokeInterceptor.CALLS);
            assertTrue(calls.stream().anyMatch(it -> it.equals("aroundInvoke onSchedule")),
                "an interceptor without an @AroundTimeout method keeps interposing: " + calls);
        }
    }
}
