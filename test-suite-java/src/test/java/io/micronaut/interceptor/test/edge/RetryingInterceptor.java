package io.micronaut.interceptor.test.edge;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Recovers from a failure of what it interposes on by proceeding a second time.
 */
@Interceptor
@Retrying
public class RetryingInterceptor {

    @AroundInvoke
    public Object retry(InvocationContext context) throws Exception {
        Log.RECORDED.add("retry");
        try {
            return context.proceed();
        } catch (IllegalStateException e) {
            Log.RECORDED.add("retry again");
            return context.proceed();
        }
    }
}
