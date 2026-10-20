package io.micronaut.interceptor.test.timeout;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.ScheduledTimer;
import io.micronaut.scheduling.ScheduledExecution;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Named;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code getTimer()} stands for the timer that invoked the timeout method. A method with two schedules
 * is invoked by each of them, and each invocation should show its own; a direct call by the application is a
 * business method invocation, interposed on by {@code @AroundInvoke}.
 */
class ScheduledTimerTest {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();
    static final List<ScheduledTimer> TIMERS = new CopyOnWriteArrayList<>();
    static volatile CountDownLatch bothRan = new CountDownLatch(2);
    // counted down by the direct call made before the schedule runs, and by the schedule
    static volatile CountDownLatch laterRan = new CountDownLatch(2);
    // counted down by each execution of the recursive schedule once its nested call returned
    static volatile CountDownLatch recursionRan = new CountDownLatch(2);
    // counted down by the execution of the schedule that follows the one that threw
    static volatile CountDownLatch afterThrowRan = new CountDownLatch(1);
    // counted down by each schedule handing a call to itself to another thread, once that call returned
    static volatile CountDownLatch handedOffRan = new CountDownLatch(2);

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

    /**
     * A schedule that names no scheduler is run by the one Micronaut runs every schedule on by default, and the timer
     * names it rather than leaving the name empty.
     */
    @Test
    void theTimerNamesTheDefaultScheduler() throws Exception {
        TIMERS.clear();
        bothRan = new CountDownLatch(2);
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(bothRan.await(5, TimeUnit.SECONDS), "both schedules ran");

            assertTrue(TIMERS.size() >= 2, "a timer per schedule: " + TIMERS);
            for (ScheduledTimer timer : TIMERS) {
                assertEquals(TaskExecutors.SCHEDULED, timer.scheduler(), timer.toString());
            }
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

    /**
     * The scheduled method calling itself: the call the scheduler made is a timeout, the nested one a business method
     * invocation with no timer, and the next execution of the schedule is a timeout again.
     */
    @Test
    void aCallTheScheduledMethodMakesToItselfIsABusinessMethodInvocation() throws Exception {
        CALLS.clear();
        recursionRan = new CountDownLatch(2);
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(RecursiveScheduledService.class);
            assertTrue(recursionRan.await(5, TimeUnit.SECONDS), "the schedule ran twice");

            List<String> calls = CALLS.stream()
                .filter(call -> call.contains("RecursiveScheduledService"))
                .map(call -> call.startsWith("timeout") && !call.endsWith(" null") ? "timeout" : call)
                .limit(4)
                .toList();
            assertEquals(List.of("timeout", "invoke RecursiveScheduledService",
                "timeout", "invoke RecursiveScheduledService"), calls);
        }
    }

    /**
     * An execution of the scheduler whose invocation throws leaves the propagated context as it found it: within the
     * execution, the context is the one the scheduler propagated, without the mark of the execution having been
     * entered. A direct call made afterwards on the same thread is a business method invocation with no timer, and
     * the next execution of the scheduler is a timeout again.
     *
     * <p>The scheduler restores the context of its thread once the method returns, which would hide a mark left
     * behind, so the executions are entered here the way the scheduler enters them, on the thread of the test.</p>
     */
    @Test
    void aScheduledInvocationThatThrowsClearsItsMark() {
        try (ApplicationContext context = ApplicationContext.run()) {
            CALLS.clear();
            FailingScheduledService service = context.getBean(FailingScheduledService.class);
            ExecutableMethod<FailingScheduledService, ?> method =
                context.getBeanDefinition(FailingScheduledService.class).getRequiredMethod("onSchedule");

            try (PropagatedContext.Scope ignored = PropagatedContext.getOrEmpty().plus(execution(method)).propagate()) {
                PropagatedContext scheduled = PropagatedContext.get();
                service.fail = true;
                assertThrows(IllegalStateException.class, service::onSchedule);
                service.fail = false;
                assertSame(scheduled, PropagatedContext.get(), "the mark of the execution is cleared once it throws");
            }

            service.onSchedule();

            try (PropagatedContext.Scope ignored = PropagatedContext.getOrEmpty().plus(execution(method)).propagate()) {
                service.onSchedule();
            }

            assertEquals(List.of("timeout", "invoke FailingScheduledService", "timeout"),
                CALLS.stream()
                    .filter(call -> call.contains("FailingScheduledService"))
                    .map(call -> call.startsWith("timeout") && !call.endsWith(" null") ? "timeout" : call)
                    .toList());
        }
    }

    /**
     * The scheduler itself running a method that throws on its first execution: the next execution is a timeout
     * again, and a direct call afterwards a business method invocation.
     */
    @Test
    void theExecutionAfterOneThatThrewIsATimeout() throws Exception {
        CALLS.clear();
        afterThrowRan = new CountDownLatch(1);
        try (ApplicationContext context = ApplicationContext.run()) {
            ThrowingScheduledService service = context.getBean(ThrowingScheduledService.class);
            assertTrue(afterThrowRan.await(5, TimeUnit.SECONDS), "the schedule ran again after throwing");
            service.onSchedule();

            List<String> calls = CALLS.stream()
                .filter(call -> call.contains("ThrowingScheduledService"))
                .map(call -> call.startsWith("timeout") && !call.endsWith(" null") ? "timeout" : call)
                .toList();
            // the schedule keeps running while the direct call is made, so only the first two calls are its alone
            assertEquals(List.of("timeout", "timeout"), calls.subList(0, 2));
            assertEquals(List.of("invoke ThrowingScheduledService"),
                calls.stream().filter(call -> !call.equals("timeout")).toList());
        }
    }

    /**
     * Work a scheduled method hands to another thread with the propagated context carries the execution of the
     * scheduler along with its mark: a call that work makes to the same scheduled method is a business method
     * invocation, whether the context is propagated by an executor of Micronaut's or by wrapping the work.
     */
    @Test
    void aCallHandedToAnotherThreadIsABusinessMethodInvocation() throws Exception {
        CALLS.clear();
        handedOffRan = new CountDownLatch(2);
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(ExecutorHandingOffService.class);
            context.getBean(WrappingHandingOffService.class);
            assertTrue(handedOffRan.await(5, TimeUnit.SECONDS), "both schedules handed a call off");

            for (String service : List.of("ExecutorHandingOffService", "WrappingHandingOffService")) {
                assertEquals(List.of("timeout", "invoke " + service),
                    CALLS.stream()
                        .filter(call -> call.contains(service))
                        .map(call -> call.startsWith("timeout") && !call.endsWith(" null") ? "timeout" : call)
                        .limit(2)
                        .toList(),
                    service);
            }
        }
    }

    private static void await(Future<?> handedOff) {
        try {
            handedOff.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("The call handed to another thread failed", e);
        }
    }

    private static ScheduledExecution execution(ExecutableMethod<?, ?> method) {
        return new ScheduledExecution(method, method.getAnnotationValuesByType(Scheduled.class).get(0));
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
            // an around-invoke method is given no timer, and the trace shows one only if it is
            CALLS.add("invoke " + context.getMethod().getDeclaringClass().getSimpleName()
                + (context.getTimer() == null ? "" : " " + context.getTimer()));
            return context.proceed();
        }

        @AroundTimeout
        public Object timeout(InvocationContext context) throws Exception {
            if (context.getTimer() instanceof ScheduledTimer timer) {
                TIMERS.add(timer);
            }
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

    @Singleton
    @TwiceTimed
    public static class RecursiveScheduledService {

        private volatile boolean nested;

        @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
        public void onSchedule() {
            if (!nested) {
                nested = true;
                try {
                    onSchedule();
                } finally {
                    nested = false;
                }
                recursionRan.countDown();
            }
        }
    }

    @Singleton
    @TwiceTimed
    public static class FailingScheduledService {

        volatile boolean fail;

        @Scheduled(fixedDelay = "1h", initialDelay = "1h")
        public void onSchedule() {
            if (fail) {
                throw new IllegalStateException("failed");
            }
        }
    }

    @Singleton
    @TwiceTimed
    public static class ThrowingScheduledService {

        private volatile int executions;

        @Scheduled(fixedDelay = "20ms", initialDelay = "10ms")
        public void onSchedule() {
            if (executions++ == 0) {
                throw new IllegalStateException("the first execution fails");
            }
            afterThrowRan.countDown();
        }
    }

    @Singleton
    @TwiceTimed
    public static class ExecutorHandingOffService {

        private final ExecutorService executor;
        private volatile boolean handedOff;

        public ExecutorHandingOffService(@Named(TaskExecutors.IO) ExecutorService executor) {
            this.executor = executor;
        }

        @Scheduled(fixedDelay = "1h", initialDelay = "10ms")
        public void onSchedule() {
            if (!handedOff) {
                handedOff = true;
                await(executor.submit(this::onSchedule));
                handedOffRan.countDown();
            }
        }
    }

    @Singleton
    @TwiceTimed
    public static class WrappingHandingOffService {

        private volatile boolean handedOff;

        @Scheduled(fixedDelay = "1h", initialDelay = "10ms")
        public void onSchedule() {
            if (!handedOff) {
                handedOff = true;
                // a plain executor, which propagates nothing on its own
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try {
                    await(executor.submit(PropagatedContext.wrapCurrent((Runnable) this::onSchedule)));
                } finally {
                    executor.shutdown();
                }
                handedOffRan.countDown();
            }
        }
    }
}
