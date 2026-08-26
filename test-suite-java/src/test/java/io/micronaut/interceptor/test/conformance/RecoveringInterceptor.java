package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.5 ba) and bb): an interceptor may catch and suppress an exception, and may recover by calling
 * {@code proceed} again.
 */
@Interceptor
@Recovering
public class RecoveringInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        try {
            return context.proceed();
        } catch (IllegalStateException e) {
            Calls.RECORDED.add("suppressed " + e.getMessage());
            return context.proceed();
        }
    }
}
