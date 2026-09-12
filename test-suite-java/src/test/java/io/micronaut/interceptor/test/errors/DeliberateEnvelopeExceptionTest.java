package io.micronaut.interceptor.test.errors;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.reflect.exception.InvocationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What an interceptor method throws travels as it was thrown, including where what it throws happens to look like
 * the envelope Micronaut puts the exception of a reflectively reached method in.
 *
 * <p>That envelope is taken off a reflectively reached interceptor method, so that what the method threw reaches the
 * caller - see {@link PrivateInterceptorMethodExceptionsTest}. An interceptor method reached directly threw what it
 * threw, and nothing is taken off it.</p>
 */
class DeliberateEnvelopeExceptionTest {

    @Test
    void leavesTheExceptionOfADirectlyReachedInterceptorMethodAlone() {
        try (ApplicationContext context = ApplicationContext.run()) {
            EnvelopedService service = context.createBean(EnvelopedService.class);

            InvocationException thrown = assertThrows(InvocationException.class, service::work);

            assertEquals("thrown by the interceptor itself", thrown.getMessage());
            assertSame(EnvelopeThrowingInterceptor.CAUSE, thrown.getCause());
        }
    }
}
