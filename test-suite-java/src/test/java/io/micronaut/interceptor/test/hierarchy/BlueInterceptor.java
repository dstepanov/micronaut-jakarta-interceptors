package io.micronaut.interceptor.test.hierarchy;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Blue
public class BlueInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("blue");
        return context.proceed();
    }
}
