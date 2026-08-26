package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Audited2
public class Audited2ConstructInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("audited");
        context.proceed();
    }
}
