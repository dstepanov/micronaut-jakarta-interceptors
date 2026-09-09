package io.micronaut.interceptor.test.ordering;

import io.micronaut.core.annotation.Order;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Ordered before its namesake, which its class name would order after it. */
@Interceptor
@Ranked
@Order(10)
public class ZuluRankedInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("zulu");
        return context.proceed();
    }
}
