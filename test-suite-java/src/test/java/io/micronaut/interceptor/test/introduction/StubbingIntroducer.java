package io.micronaut.interceptor.test.introduction;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;

/**
 * Implements an abstract method by returning a value of its own, and proceeds into a concrete one.
 */
@Singleton
@InterceptorBean(Stubbed.class)
public class StubbingIntroducer implements MethodInterceptor<Object, Object> {

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Calls.RECORDED.add("introducer " + context.getMethodName());
        if (context.isAbstract()) {
            return "introduced";
        }
        return context.proceed();
    }
}
