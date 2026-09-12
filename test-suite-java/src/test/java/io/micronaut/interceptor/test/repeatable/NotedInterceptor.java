package io.micronaut.interceptor.test.repeatable;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Bound by one occurrence of the repeatable binding, saying nothing about the member excluded from it. */
@Interceptor
@Noted("one")
public class NotedInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("noted");
        return context.proceed();
    }
}
