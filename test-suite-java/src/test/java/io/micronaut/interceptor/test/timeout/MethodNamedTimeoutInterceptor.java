package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Named by a scheduled method rather than by its class. */
public class MethodNamedTimeoutInterceptor {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        CALLS.add(context.getMethod().getName());
        return context.proceed();
    }
}
