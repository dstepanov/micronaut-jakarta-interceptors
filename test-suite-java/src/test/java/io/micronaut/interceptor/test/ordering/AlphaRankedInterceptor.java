package io.micronaut.interceptor.test.ordering;

import io.micronaut.core.annotation.Order;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Declares no priority of the specification, and an order that puts it after its namesake - which its class name
 * would put it before, so that the name cannot be what ordered them.
 */
@Interceptor
@Ranked
@Order(20)
public class AlphaRankedInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("alpha");
        return context.proceed();
    }
}
