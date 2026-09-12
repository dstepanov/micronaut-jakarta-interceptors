package io.micronaut.interceptor.test.destruction;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.interceptor.Interceptor;

/**
 * An ordinary Micronaut interceptor on the pre-destroy event, ordered before the Jakarta advice, which returns
 * without proceeding. Nothing ordered after it runs, the Jakarta pre-destroy interception among it.
 */
@InterceptorBean(SuppressedDestroyAdvice.class)
public class SuppressedDestroyInterceptor implements MethodInterceptor<Object, Object> {

    @Override
    public int getOrder() {
        return Interceptor.Priority.APPLICATION - 100;
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Destructions.RECORDED.add("micronaut pre-destroy advice suppressed the rest");
        return context.getTarget();
    }
}
