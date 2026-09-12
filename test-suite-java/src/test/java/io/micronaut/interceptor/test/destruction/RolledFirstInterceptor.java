package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.Priority;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** The interceptor of {@link RolledService} that is created first, and succeeds. */
@Interceptor
@Rolled
@Priority(Interceptor.Priority.APPLICATION - 10)
public class RolledFirstInterceptor {

    public RolledFirstInterceptor() {
        Destructions.RECORDED.add("rolled first created");
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        return context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("rolled first destroyed");
    }
}
