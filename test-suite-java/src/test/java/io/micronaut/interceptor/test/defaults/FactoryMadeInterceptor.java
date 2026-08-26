package io.micronaut.interceptor.test.defaults;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor class with no {@code @Interceptor} of its own, produced by a factory method instead.
 */
public class FactoryMadeInterceptor {

    private final String name;

    public FactoryMadeInterceptor(String name) {
        this.name = name;
    }

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Log.RECORDED.add("factory made interceptor " + name);
        return context.proceed();
    }
}
