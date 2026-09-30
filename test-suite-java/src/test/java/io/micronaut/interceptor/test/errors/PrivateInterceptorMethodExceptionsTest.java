package io.micronaut.interceptor.test.errors;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.scheduling.ScheduledExecution;
import io.micronaut.scheduling.annotation.Scheduled;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A private interceptor method is reached reflectively. What it throws still travels through the chain as it was
 * thrown, to the interceptors before it and to the caller, whatever kind of interception the method interposes on.
 */
class PrivateInterceptorMethodExceptionsTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @BeforeEach
    void reset() {
        PrivateGuardInterceptor.refuse = false;
        PrivateGuardInterceptor.failure = null;
        ObservingInterceptor.seen = null;
    }

    @Test
    void aCheckedExceptionOfAPrivateAroundInvokeMethodTravelsUnchanged() {
        PrivateGuardInterceptor.refuse = true;
        PrivatelyGuardedService service = context.createBean(PrivatelyGuardedService.class);

        RefusedException refused = assertThrows(RefusedException.class, service::checked);

        assertEquals("refused privately", refused.getMessage());
        assertSame(refused, ObservingInterceptor.seen);
    }

    @Test
    void anExceptionOfTheInterceptedMethodTravelsThroughAPrivateAroundInvokeMethodUnchanged() {
        PrivatelyGuardedService service = context.createBean(PrivatelyGuardedService.class);

        IllegalStateException failure = assertThrows(IllegalStateException.class, service::failing);

        assertEquals("from the target", failure.getMessage());
        assertSame(failure, ObservingInterceptor.seen);
    }

    @Test
    void anExceptionOfAPrivateAroundTimeoutMethodTravelsUnchanged() {
        IllegalArgumentException thrown = new IllegalArgumentException("from the timeout interceptor");
        PrivateGuardInterceptor.failure = thrown;
        PrivatelyGuardedSchedule schedule = context.getBean(PrivatelyGuardedSchedule.class);
        // the scheduler invokes the method with its invocation in the propagated context, which is what makes the
        // call a timeout
        ExecutableMethod<PrivatelyGuardedSchedule, Object> method = context.getBeanDefinition(PrivatelyGuardedSchedule.class)
            .getRequiredMethod("onSchedule");
        AnnotationValue<Scheduled> trigger = method.getAnnotationValuesByType(Scheduled.class).get(0);
        PropagatedContext scheduled = PropagatedContext.getOrEmpty().plus(new ScheduledExecution(method, trigger));

        assertSame(thrown, assertThrows(IllegalArgumentException.class, () -> scheduled.propagate(schedule::onSchedule)));
    }

    @Test
    void anExceptionOfAPrivatePostConstructMethodReachesTheInterceptorBeforeItUnchanged() {
        IllegalArgumentException thrown = new IllegalArgumentException("from the lifecycle interceptor");
        PrivateGuardInterceptor.failure = thrown;

        context.createBean(PrivatelyGuardedService.class);

        assertInstanceOf(IllegalArgumentException.class, ObservingInterceptor.seen);
        assertSame(thrown, ObservingInterceptor.seen);
    }
}
