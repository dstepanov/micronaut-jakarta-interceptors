package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.2 ac): an interceptor method may be defined in a superclass of the target class.
 */
public abstract class BaseTarget {

    @AroundInvoke
    Object declaredOnTheSuperclass(InvocationContext context) throws Exception {
        Calls.RECORDED.add("superclass interceptor method");
        return context.proceed();
    }
}
