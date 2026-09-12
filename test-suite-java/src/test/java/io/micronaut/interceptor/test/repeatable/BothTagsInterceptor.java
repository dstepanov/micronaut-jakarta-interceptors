package io.micronaut.interceptor.test.repeatable;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** An interceptor bound by two occurrences of the repeatable binding, both of which an element has to carry. */
@Interceptor
@Tag("one")
@Tag("two")
public class BothTagsInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("both");
        return context.proceed();
    }
}
