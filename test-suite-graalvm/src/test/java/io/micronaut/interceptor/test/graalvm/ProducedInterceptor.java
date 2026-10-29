package io.micronaut.interceptor.test.graalvm;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Reads the binding a factory member declares, and the annotation one of its members returns.
 */
@Interceptor
@Produced
public class ProducedInterceptor {
    @AroundInvoke
    Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("produced " + context.getInterceptorBinding(Produced.class).origin().value());
        return context.proceed();
    }
}
