package io.micronaut.interceptor.test.binding;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Watched
public class WatchingInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("watched " + context.getMethod().getName());
        return context.proceed();
    }
}
