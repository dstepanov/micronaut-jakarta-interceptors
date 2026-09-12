package io.micronaut.interceptor.test.repeatable;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** An interceptor bound by one occurrence of the repeatable binding. */
@Interceptor
@Tag("one")
public class OneTagInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("one");
        return context.proceed();
    }
}
