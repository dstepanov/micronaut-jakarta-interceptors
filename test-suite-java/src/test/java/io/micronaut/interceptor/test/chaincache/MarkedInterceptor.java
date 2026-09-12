package io.micronaut.interceptor.test.chaincache;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Marked
public class MarkedInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("bound interceptor");
        return context.proceed();
    }
}
