package io.micronaut.interceptor.test.edge;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;

/**
 * An ordinary Micronaut interceptor, to see that it composes with the Jakarta Interceptors of the same bean.
 */
@InterceptorBean(MicronautAdvice.class)
public class NativeMicronautInterceptor implements MethodInterceptor<Object, Object> {

    @Override
    public int getOrder() {
        return jakarta.interceptor.Interceptor.Priority.APPLICATION - 100;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Log.RECORDED.add("micronaut advice in");
        try {
            return context.proceed();
        } finally {
            Log.RECORDED.add("micronaut advice out");
        }
    }
}
