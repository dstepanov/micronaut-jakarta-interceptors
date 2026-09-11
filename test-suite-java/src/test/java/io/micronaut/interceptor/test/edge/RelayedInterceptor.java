package io.micronaut.interceptor.test.edge;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Relayed
public class RelayedInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Log.RECORDED.add("intercepted " + context.getMethod().getName());
        return context.proceed();
    }
}
