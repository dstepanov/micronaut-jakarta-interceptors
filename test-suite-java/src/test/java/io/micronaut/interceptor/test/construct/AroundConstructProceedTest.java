
package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AroundConstructProceedTest {

    /** 2.5: an around-construct interceptor method may throw a checked exception, which reaches the caller as it is. */
    @Test
    void aCheckedExceptionOfTheInterceptorReachesTheCaller() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Exception thrown = assertThrows(Exception.class, () -> context.createBean(FragileService.class));
            assertTrue(causes(thrown).contains(IOException.class), () -> "not an IOException: " + thrown);
        }
    }

    /** 2.3: the constructor runs once, however many times the interceptor proceeds. */
    @Test
    void proceedingTwiceConstructsOnce() {
        InsistentService.CONSTRUCTIONS.set(0);
        try (ApplicationContext context = ApplicationContext.run()) {
            context.createBean(InsistentService.class);
            assertEquals(1, InsistentService.CONSTRUCTIONS.get());
        }
    }

    private static java.util.List<Class<?>> causes(Throwable thrown) {
        java.util.List<Class<?>> causes = new java.util.ArrayList<>();
        for (Throwable t = thrown; t != null && !causes.contains(t.getClass()); t = t.getCause()) {
            causes.add(t.getClass());
        }
        return causes;
    }
}
