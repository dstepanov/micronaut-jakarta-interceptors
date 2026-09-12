package io.micronaut.interceptor.test.identity;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Logged
public class LoggingInterceptor {

    @AroundConstruct
    public void constructed(InvocationContext context) throws Exception {
        context.proceed();
    }

    @PostConstruct
    public void initialized(InvocationContext context) throws Exception {
        context.proceed();
    }

    @AroundInvoke
    public Object log(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
