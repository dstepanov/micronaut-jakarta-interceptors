package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
        record("postConstruct", context);
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        record("preDestroy", context);
        context.proceed();
    }

    /**
     * A lifecycle callback has no arguments to replace, so setParameters fails as getParameters does. Recorded
     * for both events, and the event is proceeded afterwards to show that the failure left it usable.
     */
    private static void record(String event, InvocationContext context) {
        try {
            context.setParameters(new Object[0]);
            Calls.RECORDED.add(event + " setParameters -> did not fail");
        } catch (IllegalStateException e) {
            Calls.RECORDED.add(event + " setParameters -> IllegalStateException");
        }
    }
}
