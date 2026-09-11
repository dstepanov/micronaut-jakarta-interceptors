package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor without a scope of its own, so an instance of it belongs to one intercepted object. The
 * {@code @PreDestroy} method takes no {@code InvocationContext}: it is the callback of the interceptor itself, not
 * an interceptor method.
 */
@Interceptor
@Owned
public class OwningInterceptor {

    private final InterceptorResource resource;

    public OwningInterceptor(InterceptorResource resource) {
        this.resource = resource;
        Destructions.RECORDED.add("interceptor created");
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        Destructions.RECORDED.add("around");
        return context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("interceptor destroyed");
    }
}
