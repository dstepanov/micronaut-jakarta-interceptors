package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Zone("a")
public class ZoneaConstructInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("zone a");
        context.proceed();
    }
}
