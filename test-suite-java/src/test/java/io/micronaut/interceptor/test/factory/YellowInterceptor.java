package io.micronaut.interceptor.test.factory;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Yellow
public class YellowInterceptor {

    @PostConstruct
    public void postConstruct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("yellow post");
        context.proceed();
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        Calls.RECORDED.add("yellow invoke");
        return context.proceed();
    }
}
