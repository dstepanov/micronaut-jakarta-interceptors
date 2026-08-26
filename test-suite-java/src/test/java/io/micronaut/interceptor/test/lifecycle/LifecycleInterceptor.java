package io.micronaut.interceptor.test.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.List;

@Interceptor
@Managed
public class LifecycleInterceptor {

    public static final List<String> CALLS = new ArrayList<>();

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        CALLS.add("interceptor postConstruct, target is a " + (context.getTarget() instanceof ManagedService ? "ManagedService" : "?"));
        context.proceed();
        CALLS.add("interceptor postConstruct done");
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        CALLS.add("interceptor preDestroy");
        context.proceed();
    }
}
