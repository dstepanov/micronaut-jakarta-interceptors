package io.micronaut.interceptor.test.errors;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionsTest {

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
        GuardInterceptor.refuse = false;
        GuardInterceptor.sawException = false;
        GuardedService.ran = false;
    }

    @Test
    void aCheckedExceptionOfAnInterceptorReachesTheCallerUnchanged() {
        GuardInterceptor.refuse = true;

        RefusedException refused = assertThrows(RefusedException.class,
            () -> context.getBean(GuardedService.class).checked());

        assertEquals("refused checked", refused.getMessage());
        assertFalse(GuardedService.ran, "the intercepted method must not run when the interceptor does not proceed");
    }

    @Test
    void anExceptionOfTheInterceptedMethodTravelsThroughTheChain() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
            () -> context.getBean(GuardedService.class).failing());

        assertEquals("from the target", failure.getMessage());
        assertTrue(GuardInterceptor.sawException, "the interceptor must be able to catch what the method threw");
    }
}
