package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Declares private interceptor methods of the same names and signatures as the ones of its subclass. A private
 * method is not overridden, so the specification invokes both.
 */
public class BaseLayerInterceptor {

    @PostConstruct
    private void postConstruct(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("base post");
        context.proceed();
    }

    @AroundInvoke
    private Object around(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("base around");
        return context.proceed();
    }
}
