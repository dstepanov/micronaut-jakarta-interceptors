package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** An interceptor declared a singleton, so one instance intercepts every object it is bound to. */
@Singleton
@Interceptor
@Shared
public class SharedInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        return context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("shared interceptor destroyed");
    }
}
