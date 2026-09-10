package io.micronaut.interceptor.test.conformance;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Primitives(bools = {true, false}, bytes = {1, 2}, shorts = {3, 4}, chars = {'a', 'b'},
    ints = {5, 6}, longs = {7L, 8L}, floats = {1.5f, 2.5f}, doubles = {3.5, 4.5})
public class PrimitiveArraysInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add(context.getMethod().getName());
        return context.proceed();
    }
}
