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

    /**
     * 2.8 c): an around-timeout method may be private.
     */
    @Test
    void interposesOnAScheduledMethodThroughAPrivateAroundTimeout() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(PrivatelyTimedService.RAN.await(5, TimeUnit.SECONDS), "the schedule did not run");

            List<String> calls = List.copyOf(PrivateTimeoutInterceptor.CALLS);
            assertTrue(calls.contains("private aroundTimeout onSchedule"), calls.toString());
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

    /**
     * Section 4 b) of the specification: an interceptor named on a method is associated with that method, for a
     * timeout method as for a business method. The other timeout test names its interceptor on the class, where
     * every method of the class would be associated with it whether the association was read from the method or
     * not; this one schedules a second method that names none.
     */
    @Test
    void namesATimeoutInterceptorOnTheMethodRatherThanOnTheClass() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(MethodNamedScheduledService.class);
            assertTrue(MethodNamedScheduledService.NAMED_RAN.await(5, TimeUnit.SECONDS),
                "the schedule of the named method did not run");
            assertTrue(MethodNamedScheduledService.PLAIN_RAN.await(5, TimeUnit.SECONDS),
                "the schedule of the other method did not run");

            List<String> calls = List.copyOf(MethodNamedTimeoutInterceptor.CALLS);
            assertTrue(calls.contains("named"), "the method that names it is interposed on: " + calls);
            assertTrue(calls.stream().noneMatch("plain"::equals),
                "the method that does not name it is not: " + calls);
        }
    }

    /**
     * Sections 2.8 ba), c) and d): an around-timeout method may be protected or have package access, may be
     * inherited from a superclass of the interceptor class - the superclass's invoked first - and the interceptor
     * may take injection and use what it was given. The kit has no timeout scenario at all, so these are held to
     * the specification here or nowhere.
     */
    @Test
    void interposesThroughAroundTimeoutMethodsHoweverTheyAreDeclared() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(VariedScheduledService.RAN.await(5, TimeUnit.SECONDS), "the schedule did not run");

            List<String> calls = List.copyOf(VariedCalls.RECORDED);
            // the first run of the schedule, up to and including the method itself
            List<String> firstRun = calls.subList(0, calls.indexOf("scheduled") + 1);

            assertTrue(firstRun.contains("protected"), "a protected around-timeout method: " + firstRun);
            assertTrue(firstRun.contains("package-private"), "a package-private one: " + firstRun);
            assertTrue(firstRun.contains("collaborator"), "an interceptor that uses what it was given: " + firstRun);
            assertTrue(firstRun.indexOf("inherited") >= 0 && firstRun.indexOf("inherited") < firstRun.indexOf("declared"),
                "an inherited around-timeout method, before the one the subclass declares: " + firstRun);
            assertEquals("scheduled", firstRun.get(firstRun.size() - 1),
                "every interceptor before the scheduled method: " + firstRun);
        }
    }

    /**
     * Section 2.3.1 b) of the specification: an around-timeout interceptor method runs in the same Java thread as
     * the timeout method it interposes on. InvocationContextConformanceTest holds an around-invoke method to it; a
     * timeout method is invoked by the scheduler on a thread of its own, which is what makes the timeout half worth
     * asserting apart. That thread differing from the thread of the test is expected and not what is compared.
     */
    @Test
    void runsAnAroundTimeoutMethodInTheSameThreadAsTheTimeoutMethod() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(ThreadedScheduledService.RAN.await(5, TimeUnit.SECONDS), "the schedule did not run");

            Thread interceptor = ThreadedScheduledService.INTERCEPTOR_THREAD.get();
            Thread method = ThreadedScheduledService.METHOD_THREAD.get();
            assertTrue(interceptor != null, "the around-timeout method ran");
            assertTrue(interceptor == method,
                "the interceptor ran on [" + interceptor + "] and the timeout method on [" + method + "]");
        }
    }
}