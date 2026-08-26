package io.micronaut.interceptor.test.graalvm;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class NamedInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("named");
        return context.proceed();
    }
}
