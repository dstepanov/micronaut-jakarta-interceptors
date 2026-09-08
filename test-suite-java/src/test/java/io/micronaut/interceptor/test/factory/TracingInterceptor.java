package io.micronaut.interceptor.test.factory;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Traced
public class TracingInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("traced");
        return context.proceed();
    }
}
