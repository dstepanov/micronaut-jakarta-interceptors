package io.micronaut.interceptor.test.reflection;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;

/**
 * The same interceptor as {@link MeasuringInterceptor}, with a private interceptor method.
 */
@Interceptor
@PrivatelyMeasured
public class PrivateMeasuringInterceptor {

    public static List<String> frames = List.of();

    @AroundInvoke
    private Object measure(InvocationContext context) throws Exception {
        frames = Frames.current();
        return context.proceed();
    }
}
