package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public abstract class ParentInterceptor extends GrandparentInterceptor {

    @AroundInvoke
    public Object parentAround(InvocationContext context) throws Exception {
        Calls.RECORDED.add("parent around");
        return context.proceed();
    }
}
