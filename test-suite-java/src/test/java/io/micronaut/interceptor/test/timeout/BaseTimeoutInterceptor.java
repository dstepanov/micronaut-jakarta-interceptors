package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

/** Declares an around-timeout method that an interceptor class inherits. */
public abstract class BaseTimeoutInterceptor {

    @AroundTimeout
    public Object inheritedAroundTimeout(InvocationContext context) throws Exception {
        VariedCalls.RECORDED.add("inherited");
        return context.proceed();
    }
}
