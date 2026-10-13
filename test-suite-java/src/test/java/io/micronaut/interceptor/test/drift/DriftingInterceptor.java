package io.micronaut.interceptor.test.drift;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Drifting
@Priority(Interceptor.Priority.APPLICATION)
public class DriftingInterceptor {

    @AroundInvoke
    Object invoke(InvocationContext context) throws Exception {
        return "drifting " + context.proceed();
    }
}
