
package io.micronaut.interceptor.test.destruction;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor every intercepted object shares, which the destruction of one of them must not take away from
 * the others.
 */
@Singleton
@Interceptor
@Shared
public class SharedInterceptor {

    @Inject
    SharedResource resource;

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
