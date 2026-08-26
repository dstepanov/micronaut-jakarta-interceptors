package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A timeout interceptor reached through {@code @Interceptors} rather than through a binding annotation.
 */
public class NamedTimeoutInterceptor {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        CALLS.add("named aroundTimeout " + context.getMethod().getName());
        return context.proceed();
    }
}
