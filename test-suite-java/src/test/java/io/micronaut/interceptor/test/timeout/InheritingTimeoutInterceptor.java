package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.8 ba): an around-timeout method may be inherited, and the one its superclass declares is invoked
 * before the one it declares itself.
 */
@Interceptor
@Varied
public class InheritingTimeoutInterceptor extends BaseTimeoutInterceptor {

    @AroundTimeout
    public Object declaredAroundTimeout(InvocationContext context) throws Exception {
        VariedCalls.RECORDED.add("declared");
        return context.proceed();
    }
}
