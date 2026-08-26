package io.micronaut.interceptor.test.variations;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class ConstructionRecordingInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("interceptor aroundConstruct");
        context.proceed();
    }

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("interceptor aroundInvoke");
        return context.proceed();
    }
}
