package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Section 2.8 c): an around-timeout method may have private access.
 */
@Interceptor
@PrivatelyTimed
public class PrivateTimeoutInterceptor {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @AroundTimeout
    private Object aroundTimeout(InvocationContext context) throws Exception {
        CALLS.add("private aroundTimeout " + context.getMethod().getName());
        return context.proceed();
    }
}
