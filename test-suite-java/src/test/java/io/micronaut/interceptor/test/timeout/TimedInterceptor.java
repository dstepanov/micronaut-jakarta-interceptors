package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An interceptor declaring both kinds: the specification has {@code @AroundInvoke} interpose on business methods
 * and {@code @AroundTimeout} on the methods a timer service invokes.
 */
@Interceptor
@Timed
public class TimedInterceptor {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        CALLS.add("aroundInvoke " + context.getMethod().getName());
        return context.proceed();
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        CALLS.add("aroundTimeout " + context.getMethod().getName() + " " + context.getTimer());
        return context.proceed();
    }
}
