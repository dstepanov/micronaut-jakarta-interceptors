package io.micronaut.interceptor.test.hierarchy;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Red
public class RedInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("red");
        return context.proceed();
    }
}
