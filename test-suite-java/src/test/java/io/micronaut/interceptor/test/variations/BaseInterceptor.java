package io.micronaut.interceptor.test.variations;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public abstract class BaseInterceptor {

    @AroundInvoke
    public Object inherited(InvocationContext context) throws Exception {
        Calls.RECORDED.add("inherited interceptor method");
        return context.proceed();
    }
}
