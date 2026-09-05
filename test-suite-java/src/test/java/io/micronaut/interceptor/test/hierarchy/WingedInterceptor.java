package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Winged
public class WingedInterceptor {

    @PostConstruct
    public void postConstruct(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("winged");
        context.proceed();
    }
}
