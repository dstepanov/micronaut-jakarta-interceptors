package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Section 5.2 i): the interceptor method of the target's superclass is invoked before the target's own.
 */
public abstract class BaseHierarchicalService {

    @AroundInvoke
    Object superclassOfTheTarget(InvocationContext context) throws Exception {
        Calls.RECORDED.add("target superclass");
        return context.proceed();
    }
}
