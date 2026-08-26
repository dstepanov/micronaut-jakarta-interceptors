package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Section 4 e): the {@code Interceptor} annotation, and the binding it carries, are ignored when the class is
 * bound to a component with {@code Interceptors}.
 */
@Interceptor
@NeverDeclared
public class BoundAndNamedInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("bound and named");
        return context.proceed();
    }
}
