package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Armed
public class ArmedInterceptor {

    @PostConstruct
    public void postConstruct(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("armed");
        context.proceed();
    }
}
