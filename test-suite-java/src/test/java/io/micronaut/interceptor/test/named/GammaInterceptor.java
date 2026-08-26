package io.micronaut.interceptor.test.named;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * A plain class, without {@code @Interceptor}: the specification lets {@code @Interceptors} name any class that
 * declares an interceptor method.
 */
public class GammaInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("Gamma");
        return context.proceed();
    }
}
