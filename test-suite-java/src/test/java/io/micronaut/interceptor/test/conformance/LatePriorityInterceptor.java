package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * A high priority, which section 5.2.1 c) has ignored when the interceptor is named with {@code Interceptors}.
 */
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class LatePriorityInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("late priority");
        return context.proceed();
    }
}
