package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@LifecycleCtx
@Priority(Interceptor.Priority.APPLICATION)
public class SecondLifecycleCtxInterceptor {

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        record("postConstruct", context);
        context.proceed();
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        record("preDestroy", context);
        context.proceed();
    }

    private static void record(String event, InvocationContext context) {
        Calls.RECORDED.add("second " + event + " context " + System.identityHashCode(context));
        Calls.RECORDED.add("second " + event + " sees " + context.getContextData().get("fromFirst"));
    }
}
