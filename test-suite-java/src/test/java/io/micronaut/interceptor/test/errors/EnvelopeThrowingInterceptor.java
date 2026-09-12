package io.micronaut.interceptor.test.errors;

import io.micronaut.core.reflect.exception.InvocationException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.InvocationTargetException;

/**
 * An interceptor whose method throws, on purpose, exactly what Micronaut wraps the exception of a reflectively
 * reached method in: an {@code InvocationException} caused by an {@code InvocationTargetException}. Its method is
 * public, so it is reached directly and nothing wrapped it.
 */
@Interceptor
@Enveloped
public class EnvelopeThrowingInterceptor {

    static final InvocationTargetException CAUSE =
        new InvocationTargetException(new IllegalStateException("what the interceptor put inside"));

    @AroundInvoke
    public Object throwTheEnvelope(InvocationContext context) {
        throw new InvocationException("thrown by the interceptor itself", CAUSE);
    }
}
