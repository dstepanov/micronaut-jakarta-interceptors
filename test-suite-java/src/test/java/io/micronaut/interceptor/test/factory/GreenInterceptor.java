package io.micronaut.interceptor.test.factory;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Green
public class GreenInterceptor {

    @PostConstruct
    public void postConstruct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("green post");
        context.proceed();
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        Calls.RECORDED.add("green invoke");
        return context.proceed();
    }
}
