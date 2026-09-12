
package io.micronaut.interceptor.test.introduction;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Interceptor
@Traced
public class TracingInterceptor {

    public static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @AroundInvoke
    public Object trace(InvocationContext context) throws Exception {
        CALLS.add(context.getMethod().getName());
        return context.proceed();
    }
}
