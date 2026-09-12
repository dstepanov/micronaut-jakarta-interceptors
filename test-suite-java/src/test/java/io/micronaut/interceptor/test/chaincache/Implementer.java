package io.micronaut.interceptor.test.chaincache;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;

/** Implements the abstract method of {@link ExpressionService} by answering for it. */
@Singleton
@InterceptorBean(Implemented.class)
public class Implementer implements MethodInterceptor<Object, Object> {

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        return "described";
    }
}
