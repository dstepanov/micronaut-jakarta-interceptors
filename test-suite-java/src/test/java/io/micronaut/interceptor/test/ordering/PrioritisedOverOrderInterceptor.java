package io.micronaut.interceptor.test.ordering;

import io.micronaut.core.annotation.Order;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Declares both. The priority of the specification puts it first; the Micronaut order alone would put it last, so
 * running first is what shows the priority to be read ahead of the order rather than beside it.
 */
@Interceptor
@Ranked
@Priority(1)
@Order(9999)
public class PrioritisedOverOrderInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("prioritised");
        return context.proceed();
    }
}
