package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@BoundToConstructor
public class ConstructorBoundInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("constructor bound interceptor");
        context.proceed();
    }
}
