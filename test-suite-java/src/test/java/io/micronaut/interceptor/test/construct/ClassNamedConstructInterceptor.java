package io.micronaut.interceptor.test.construct;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

/** Named by the class, which the specification invokes before the one the constructor names. */
public class ClassNamedConstructInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        ConstructorNamedInterceptor.CALLS.add("class named");
        context.proceed();
    }
}
