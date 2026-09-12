package io.micronaut.interceptor.test.factory;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * A factory that interposes on the business methods of its own instance and also binds the bean one of its methods
 * produces. The two are separate: the interceptor method of the factory is a method of the factory, not of the
 * product.
 */
@Factory
public class SelfInterceptingBoltFactory {

    @Prototype
    @Green
    Bolt bolt() {
        return new Bolt();
    }

    public String describe() {
        Calls.RECORDED.add("describe");
        return "factory";
    }

    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("factory self " + context.getMethod().getName());
        return context.proceed();
    }
}
