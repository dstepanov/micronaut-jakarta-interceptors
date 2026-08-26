package io.micronaut.interceptor.test.selfintercept;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class TracingInterceptor {

    @AroundInvoke
    public Object trace(InvocationContext context) throws Exception {
        SelfInterceptingService.CALLS.add("interceptor class in");
        try {
            return context.proceed();
        } finally {
            SelfInterceptingService.CALLS.add("interceptor class out");
        }
    }
}
