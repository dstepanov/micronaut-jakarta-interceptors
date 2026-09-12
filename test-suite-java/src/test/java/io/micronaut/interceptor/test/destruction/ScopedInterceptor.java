package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor in a custom scope, which Micronaut resolves through a proxy over a target the scope keeps. One
 * target serves every object the interceptor is bound to, so it belongs to the scope rather than to any one of
 * them.
 */
@Interceptor
@PerContext
@Scoped
public class ScopedInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        Destructions.RECORDED.add("scoped around");
        return context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("scoped interceptor destroyed");
    }
}
