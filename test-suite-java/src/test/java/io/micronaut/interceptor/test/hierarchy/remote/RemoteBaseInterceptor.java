package io.micronaut.interceptor.test.hierarchy.remote;

import io.micronaut.interceptor.test.hierarchy.Hierarchy;
import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Declares a protected and a package private interceptor method in a package other than the one of the interceptor
 * class that extends it. Neither is visible to code generated in the package of that subclass, so both are reached
 * reflectively.
 */
public class RemoteBaseInterceptor {

    @PostConstruct
    void remotePostConstruct(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("remote post");
        context.proceed();
    }

    @AroundInvoke
    protected Object remoteAround(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("remote around");
        return context.proceed();
    }
}
