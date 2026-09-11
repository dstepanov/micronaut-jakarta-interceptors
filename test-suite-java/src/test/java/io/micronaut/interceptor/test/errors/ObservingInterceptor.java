package io.micronaut.interceptor.test.errors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * The outer interceptor, which records what the inner one let through.
 */
@Interceptor
@PrivatelyGuarded
@Priority(Interceptor.Priority.APPLICATION + 10)
public class ObservingInterceptor {

    static volatile Throwable seen;

    @AroundInvoke
    public Object observe(InvocationContext context) throws Exception {
        try {
            return context.proceed();
        } catch (Exception e) {
            seen = e;
            throw e;
        }
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        try {
            context.proceed();
        } catch (RuntimeException e) {
            seen = e;
        }
    }
}
