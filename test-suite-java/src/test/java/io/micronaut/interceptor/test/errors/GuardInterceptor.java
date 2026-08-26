package io.micronaut.interceptor.test.errors;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Guarded
public class GuardInterceptor {

    public static boolean refuse;
    public static boolean sawException;

    @AroundInvoke
    public Object guard(InvocationContext context) throws Exception {
        if (refuse) {
            // an interceptor that does not proceed keeps the intercepted method from running
            throw new RefusedException("refused " + context.getMethod().getName());
        }
        try {
            return context.proceed();
        } catch (IllegalStateException e) {
            sawException = true;
            throw e;
        }
    }
}
