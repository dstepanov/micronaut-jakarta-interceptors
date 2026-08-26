package io.micronaut.interceptor.test.state;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Counted
public class CountingInterceptor {

    private int count;

    @AroundInvoke
    public Object count(InvocationContext context) throws Exception {
        count++;
        CountingInterceptors.seen(context.getTarget(), this);
        return context.proceed();
    }

    public int count() {
        return count;
    }
}
