package io.micronaut.interceptor.test.reflection;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;

@Interceptor
@Measured
public class MeasuringInterceptor {

    public static List<String> frames = List.of();

    @AroundInvoke
    public Object measure(InvocationContext context) throws Exception {
        frames = StackWalker.getInstance()
            .walk(stream -> stream
                .map(frame -> frame.getClassName() + "." + frame.getMethodName())
                .toList());
        return context.proceed();
    }
}
