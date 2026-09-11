package io.micronaut.interceptor.test.external;

import io.micronaut.context.annotation.Prototype;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * A bean of its own, so Micronaut generated a definition of it, but one that does not describe its interceptor
 * methods, since the Jakarta Interceptors processor never saw it.
 */
@Interceptor
@Prototype
public class UnprocessedBeanInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
