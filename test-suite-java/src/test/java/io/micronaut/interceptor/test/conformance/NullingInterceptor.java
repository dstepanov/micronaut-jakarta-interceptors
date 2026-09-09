package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Replaces the arguments with nulls, which the specification lets an interceptor do for a reference argument and
 * cannot mean for a primitive one.
 */
@Interceptor
@Nulled
public class NullingInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        try {
            context.setParameters(null);
            Calls.RECORDED.add("null array -> did not fail");
        } catch (IllegalArgumentException e) {
            Calls.RECORDED.add("null array -> IllegalArgumentException");
        }
        try {
            context.setParameters(new Object[]{null});
            Calls.RECORDED.add("null value -> accepted");
        } catch (IllegalArgumentException e) {
            Calls.RECORDED.add("null value -> IllegalArgumentException");
        }
        return context.proceed();
    }
}
