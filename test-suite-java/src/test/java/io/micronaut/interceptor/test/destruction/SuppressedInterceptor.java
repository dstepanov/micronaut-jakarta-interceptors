package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * A Jakarta interceptor of the pre-destroy event, whose interceptor method never runs because
 * {@link SuppressedDestroyInterceptor} is ordered before it and does not proceed. The instance still has to be
 * destroyed with the object it was created for.
 */
@Interceptor
@Suppressed
public class SuppressedInterceptor {

    public SuppressedInterceptor() {
        Destructions.RECORDED.add("suppressed interceptor created");
    }

    @PreDestroy
    public void beforeDestroy(InvocationContext context) throws Exception {
        Destructions.RECORDED.add("suppressed interceptor pre-destroy method");
        context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("suppressed interceptor destroyed");
    }
}
