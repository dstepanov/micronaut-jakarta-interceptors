package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class EarlyPriorityInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("early priority");
        return context.proceed();
    }
}
