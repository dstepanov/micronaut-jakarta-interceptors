package io.micronaut.interceptor.test.noreflection;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Asks for the instances of a binding whose builder was generated, which needs no reflection module.
 */
@Labelled
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LabellingInterceptor {

    @AroundInvoke
    Object invoke(InvocationContext context) throws Exception {
        Probe.attempt("registered getInterceptorBinding(Class)", () -> context.getInterceptorBinding(Labelled.class));
        Probe.attempt("registered getInterceptorBindings(Class)", () -> context.getInterceptorBindings(Labelled.class));
        Probe.attempt("registered getInterceptorBindings()", context::getInterceptorBindings);
        return context.proceed();
    }
}
