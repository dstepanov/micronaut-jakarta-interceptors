package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Recorded
public class LifecycleProceedInterceptor {

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Object result = context.proceed();
        Calls.RECORDED.add("postConstruct proceed -> " + result);
        Calls.RECORDED.add("postConstruct getMethod -> " + context.getMethod());
        Calls.RECORDED.add("postConstruct getTimer -> " + context.getTimer());
        try {
            context.getParameters();
            Calls.RECORDED.add("postConstruct getParameters -> did not fail");
        } catch (IllegalStateException e) {
            Calls.RECORDED.add("postConstruct getParameters -> IllegalStateException");
        }
    }
}
