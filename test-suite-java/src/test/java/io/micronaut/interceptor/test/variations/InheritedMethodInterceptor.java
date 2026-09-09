package io.micronaut.interceptor.test.variations;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor class whose interceptor method is inherited from its superclass, which the specification allows.
 */
@Interceptor
@Inheriting
public class InheritedMethodInterceptor extends BaseInterceptor {

    /** Declared here as well, so that the order of the two can be seen. */
    @AroundConstruct
    public void declaredConstruct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("declared aroundConstruct");
        context.proceed();
    }
}
