package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Section 5.2 d) and h): the interceptor methods of the superclasses are invoked before the one the class declares
 * itself, the most general superclass first. One interceptor instance serves them all.
 */
@Interceptor
@Hierarchical
public class HierarchyInterceptor extends ParentInterceptor {

    @AroundInvoke
    public Object ownAround(InvocationContext context) throws Exception {
        Calls.RECORDED.add("own around instance=" + System.identityHashCode(this));
        return context.proceed();
    }

    @PostConstruct
    public void ownCreated(InvocationContext context) throws Exception {
        Calls.RECORDED.add("own postConstruct");
        context.proceed();
    }
}
