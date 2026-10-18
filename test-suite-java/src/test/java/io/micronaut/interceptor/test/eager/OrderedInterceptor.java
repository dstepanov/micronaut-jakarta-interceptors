package io.micronaut.interceptor.test.eager;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Records its creation among the steps of the creation of the object it intercepts. */
@Interceptor
@Ordered
public class OrderedInterceptor {

    public OrderedInterceptor() {
        SubclassCreationTest.STEPS.add("interceptor created");
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
