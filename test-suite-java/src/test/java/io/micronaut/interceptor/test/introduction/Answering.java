
package io.micronaut.interceptor.test.introduction;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;

/**
 * Implements every method of an {@link Oracular} interface with the same answer.
 */
@InterceptorBean(Oracular.class)
public class Answering implements MethodInterceptor<Object, Object> {

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        return "42";
    }
}
