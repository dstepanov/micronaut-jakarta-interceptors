package io.micronaut.interceptor.test.edge;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class NamedFirstInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Log.RECORDED.add("named first");
        return context.proceed();
    }
}
