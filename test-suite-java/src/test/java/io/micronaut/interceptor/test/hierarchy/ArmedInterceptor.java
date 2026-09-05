package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Armed
public class ArmedInterceptor {

    @PostConstruct
    public void postConstruct(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("armed post");
        context.proceed();
    }

    @PreDestroy
    public void preDestroy(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("armed pre");
        context.proceed();
    }
}
