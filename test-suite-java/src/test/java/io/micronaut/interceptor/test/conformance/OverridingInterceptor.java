package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.InvocationContext;

/**
 * Section 5.2 j): the interceptor method of the superclass is overridden by a method that is not itself an
 * interceptor method, so it is not invoked.
 */
public class OverridingInterceptor extends BaseOverridden {

    @Override
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("the override, which is not an interceptor method");
        return context.proceed();
    }
}
