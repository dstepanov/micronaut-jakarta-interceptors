package io.micronaut.interceptor.test.introduction;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Watched
public class WatchingInterceptor {

    @AroundInvoke
    public Object watch(InvocationContext context) throws Exception {
        Calls.RECORDED.add("bound " + context.getMethod().getName());
        return context.proceed();
    }
}
