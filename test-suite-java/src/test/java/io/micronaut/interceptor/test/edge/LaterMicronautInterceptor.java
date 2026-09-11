package io.micronaut.interceptor.test.edge;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.interceptor.Interceptor;

/**
 * An ordinary Micronaut interceptor ordered after the Jakarta Interceptors of the same bean, so that it is part of
 * what their chain proceeds into.
 */
@InterceptorBean(LaterMicronautAdvice.class)
public class LaterMicronautInterceptor implements MethodInterceptor<Object, Object> {

    @Override
    public int getOrder() {
        return Interceptor.Priority.APPLICATION + 100;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Log.RECORDED.add("micronaut advice");
        return context.proceed();
    }
}
