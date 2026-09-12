package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** The interceptor of {@link RolledService} that is created second, and fails. */
@Interceptor
@Rolled
@Priority(Interceptor.Priority.APPLICATION + 10)
public class RolledSecondInterceptor {

    public RolledSecondInterceptor() {
        throw new IllegalStateException("this interceptor cannot be created");
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
