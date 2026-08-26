package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An interceptor declaring no {@code @AroundTimeout} method still interposes on a scheduled method, so that
 * scheduling a method does not quietly take its interception away.
 */
@Interceptor
@Watched
public class OnlyAroundInvokeInterceptor {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        CALLS.add("aroundInvoke " + context.getMethod().getName());
        return context.proceed();
    }
}
