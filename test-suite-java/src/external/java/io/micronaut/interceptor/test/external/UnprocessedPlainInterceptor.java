package io.micronaut.interceptor.test.external;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Not a bean, so nothing generated a definition of it at all.
 */
@Interceptor
public class UnprocessedPlainInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
