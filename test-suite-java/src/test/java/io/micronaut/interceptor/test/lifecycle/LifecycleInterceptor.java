package io.micronaut.interceptor.test.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Interceptor
@Managed
public class LifecycleInterceptor {

    public static final List<String> CALLS = new ArrayList<>();

    /** The callback of the intercepted class each interceptor method was shown. */
    public static final List<Method> CALLBACKS = new ArrayList<>();

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        CALLBACKS.add(context.getMethod());
        CALLS.add("interceptor postConstruct, target is a " + (context.getTarget() instanceof ManagedService ? "ManagedService" : "?"));
        context.proceed();
        CALLS.add("interceptor postConstruct done");
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        CALLBACKS.add(context.getMethod());
        CALLS.add("interceptor preDestroy");
        context.proceed();
    }
}
