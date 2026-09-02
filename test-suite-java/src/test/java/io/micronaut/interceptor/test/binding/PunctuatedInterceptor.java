package io.micronaut.interceptor.test.binding;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Bound by a value that reads as two members: {@code first} carries the comma and the equals sign that separate
 * one member of a binding from the next.
 */
@Interceptor
@Tagged(first = "a,second=b", second = "c")
public class PunctuatedInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("punctuated");
        return context.proceed();
    }
}
