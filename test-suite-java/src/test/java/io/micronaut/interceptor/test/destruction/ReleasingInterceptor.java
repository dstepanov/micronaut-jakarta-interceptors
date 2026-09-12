
package io.micronaut.interceptor.test.destruction;

import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor created for each object it intercepts, and destroyed with it.
 */
@Interceptor
@Released
public class ReleasingInterceptor {

    @Inject
    ReleasedResource resource;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
