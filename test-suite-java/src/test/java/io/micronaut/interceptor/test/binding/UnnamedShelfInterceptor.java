package io.micronaut.interceptor.test.binding;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Shelved
public class UnnamedShelfInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("unnamed shelf");
        return context.proceed();
    }
}
