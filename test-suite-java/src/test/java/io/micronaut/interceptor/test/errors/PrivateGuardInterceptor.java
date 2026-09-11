package io.micronaut.interceptor.test.errors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * The inner interceptor, whose interceptor methods are all private and so are reached reflectively.
 */
@Interceptor
@PrivatelyGuarded
@Priority(Interceptor.Priority.APPLICATION + 20)
public class PrivateGuardInterceptor {

    static volatile boolean refuse;
    static volatile RuntimeException failure;

    @AroundInvoke
    private Object guard(InvocationContext context) throws Exception {
        if (refuse) {
            throw new RefusedException("refused privately");
        }
        return context.proceed();
    }

    @AroundTimeout
    private Object guardTimeout(InvocationContext context) throws Exception {
        if (failure != null) {
            throw failure;
        }
        return context.proceed();
    }

    @PostConstruct
    private void created(InvocationContext context) throws Exception {
        if (failure != null) {
            throw failure;
        }
        context.proceed();
    }
}
