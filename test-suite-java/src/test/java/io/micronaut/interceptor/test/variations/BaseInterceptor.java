package io.micronaut.interceptor.test.variations;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public abstract class BaseInterceptor {

    @AroundInvoke
    public Object inherited(InvocationContext context) throws Exception {
        Calls.RECORDED.add("inherited interceptor method");
        return context.proceed();
    }

    /**
     * A constructor interceptor method is inherited as an around-invoke one is, and the specification invokes the
     * one of the superclass before the one the subclass declares.
     */
    @AroundConstruct
    public void inheritedConstruct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("inherited aroundConstruct");
        context.proceed();
    }
}
