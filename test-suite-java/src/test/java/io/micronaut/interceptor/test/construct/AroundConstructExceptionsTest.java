package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.BeanInstantiationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An around-construct interceptor method may throw what the constructor it interposes on is allowed to throw, checked
 * exceptions included, and lets through what the constructor throws. Neither is a lifecycle callback throwing a
 * checked exception it must not throw: construction is reported the way Micronaut reports a constructor that is not
 * intercepted at all.
 */
class AroundConstructExceptionsTest {

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
        RefusingConstructInterceptor.refusal = null;
        RefusingConstructInterceptor.seen = null;
        FragileBuiltService.failure = null;
        UninterceptedFragileService.failure = null;
    }

    @Test
    void aCheckedExceptionOfAnUninterceptedConstructorIsTheCauseOfTheFailure() {
        IOException failure = new IOException("from the constructor");
        UninterceptedFragileService.failure = failure;

        BeanInstantiationException thrown = assertThrows(BeanInstantiationException.class,
            () -> context.createBean(UninterceptedFragileService.class));

        assertSame(failure, thrown.getCause());
    }

    @Test
    void aCheckedExceptionOfAnInterceptedConstructorIsReportedTheSameWay() {
        IOException failure = new IOException("from the constructor");
        FragileBuiltService.failure = failure;

        BeanInstantiationException thrown = assertThrows(BeanInstantiationException.class,
            () -> context.createBean(FragileBuiltService.class));

        assertSame(failure, thrown.getCause());
        assertSame(failure, RefusingConstructInterceptor.seen, "the interceptor sees what the constructor threw");
    }

    @Test
    void aCheckedExceptionTheConstructorDeclaresMayBeThrownByTheInterceptor() {
        IOException refusal = new IOException("from the interceptor");
        RefusingConstructInterceptor.refusal = refusal;

        BeanInstantiationException thrown = assertThrows(BeanInstantiationException.class,
            () -> context.createBean(FragileBuiltService.class));

        assertSame(refusal, thrown.getCause());
    }

    @Test
    void aRuntimeExceptionOfTheInterceptorReachesTheCallerUnchanged() {
        IllegalStateException refusal = new IllegalStateException("from the interceptor");
        RefusingConstructInterceptor.refusal = refusal;

        assertSame(refusal, assertThrows(IllegalStateException.class,
            () -> context.createBean(FragileBuiltService.class)));
    }
}
