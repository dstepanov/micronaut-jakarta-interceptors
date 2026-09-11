package io.micronaut.interceptor.test.introduction;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class NamedWatchingInterceptor {

    @AroundInvoke
    public Object watch(InvocationContext context) throws Exception {
        Calls.RECORDED.add("named " + context.getMethod().getName());
        return context.proceed();
    }
}
