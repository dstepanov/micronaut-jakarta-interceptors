package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Recorded
public class VoidProceedInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Object result = context.proceed();
        if (context.getMethod().getReturnType() == void.class) {
            Calls.RECORDED.add("void proceed -> " + result);
        }
        return result;
    }
}
