package io.micronaut.interceptor.test.factory;

import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor class the application declares a bean of its own, with a scope, which the module leaves as it was
 * declared.
 */
@Singleton
@Interceptor
public class SingletonInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("singleton");
        return context.proceed();
    }
}
