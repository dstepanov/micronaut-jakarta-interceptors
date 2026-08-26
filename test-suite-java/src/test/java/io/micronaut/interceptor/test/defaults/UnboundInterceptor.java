package io.micronaut.interceptor.test.defaults;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
public class UnboundInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Log.RECORDED.add("unbound interceptor");
        return context.proceed();
    }
}
