package io.micronaut.interceptor.test.identity;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class NamedInterceptor {

    @AroundInvoke
    public Object named(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
