package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Zone("b")
public class ZonebConstructInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("zone b");
        context.proceed();
    }
}
