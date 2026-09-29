package io.micronaut.interceptor.test.errors;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.reflect.exception.InvocationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What an interceptor method throws travels as it was thrown, including where what it throws happens to look like
 * the envelope a reflective call puts an exception in.
 *
 * <p>A reflectively reached interceptor method does not throw that envelope: Micronaut rethrows what the method threw
 * instead - see {@link PrivateInterceptorMethodExceptionsTest}. Nothing is taken off what an interceptor method
 * throws, so one that throws that pair of exceptions on purpose has it delivered whole.</p>
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
