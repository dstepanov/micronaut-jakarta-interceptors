package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class BaseOverridden {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("must not be invoked");
        return context.proceed();
    }
}
