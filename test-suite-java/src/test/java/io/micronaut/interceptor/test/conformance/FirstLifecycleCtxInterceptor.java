package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@LifecycleCtx
@Priority(Interceptor.Priority.APPLICATION - 10)
public class FirstLifecycleCtxInterceptor {

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
        Calls.RECORDED.add("first " + event + " context " + System.identityHashCode(context));
        Calls.RECORDED.add("first " + event + " sees " + context.getContextData().get("fromFirst"));
        context.getContextData().put("fromFirst", "written by the first");
    }
}
