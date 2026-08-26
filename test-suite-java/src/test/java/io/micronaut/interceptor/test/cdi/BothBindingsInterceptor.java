package io.micronaut.interceptor.test.cdi;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor declaring two bindings intercepts only the elements declaring both of them.
 */
@Interceptor
@Secure
@Monitored
public class BothBindingsInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("both");
        return context.proceed();
    }
}
