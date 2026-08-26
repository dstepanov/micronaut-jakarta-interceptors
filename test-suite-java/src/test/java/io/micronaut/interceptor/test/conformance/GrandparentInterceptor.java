package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * The most general superclass of an interceptor class hierarchy. Section 5.2 d) invokes its interceptor method
 * first.
 */
public abstract class GrandparentInterceptor {

    @AroundInvoke
    public Object grandparentAround(InvocationContext context) throws Exception {
        Calls.RECORDED.add("grandparent around");
        return context.proceed();
    }

    @PostConstruct
    public void grandparentCreated(InvocationContext context) throws Exception {
        Calls.RECORDED.add("grandparent postConstruct");
        context.proceed();
    }
}
