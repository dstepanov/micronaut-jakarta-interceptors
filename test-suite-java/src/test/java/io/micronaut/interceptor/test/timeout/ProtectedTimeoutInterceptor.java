package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Section 2.8 c): an around-timeout method may be protected. */
@Interceptor
@Varied
public class ProtectedTimeoutInterceptor {

    @AroundTimeout
    protected Object aroundTimeout(InvocationContext context) throws Exception {
        VariedCalls.RECORDED.add("protected");
        return context.proceed();
    }
}
