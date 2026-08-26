package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleExceptionTest {

    @AfterEach
    void reset() {
        ThrowingCallbackInterceptor.runtimeFailure = null;
        ThrowingCallbackInterceptor.checkedFailure = null;
    }

    /** 2.7.1 b) A lifecycle callback interceptor method may catch what another one threw. */
    @Test
    void oneCallbackMayCatchWhatAnotherThrew() {
        Calls.clear();
        ThrowingCallbackInterceptor.runtimeFailure = new IllegalArgumentException("from the callback");
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(FragileService.class);

            assertEquals(List.of("throwing callback", "caught from the callback"), Calls.RECORDED);
        }
    }

    /** 2.7.1 a) A checked exception is not something a lifecycle callback may throw, and is reported as such. */
    @Test
    void reportsACheckedExceptionThrownByACallback() {
        Calls.clear();
        ThrowingCallbackInterceptor.checkedFailure = new RefusedCheckedException("not allowed here");
        try (ApplicationContext context = ApplicationContext.run()) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                () -> context.getBean(FragileService.class));

            assertTrue(rootCauseMessage(failure).contains("checked exception"), rootCauseMessage(failure));
        }
    }

    private static String rootCauseMessage(Throwable t) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = t; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append(' ');
        }
        return messages.toString();
    }
}
