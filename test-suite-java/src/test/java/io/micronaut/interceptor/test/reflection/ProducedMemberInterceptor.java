package io.micronaut.interceptor.test.reflection;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;

@Interceptor
@Produced
public class ProducedMemberInterceptor {

    public static Method businessMethod;

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        businessMethod = context.getMethod();
        return context.proceed();
    }
}
