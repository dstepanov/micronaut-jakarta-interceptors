package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Records the thread it runs on, immediately around proceeding to the timeout method. */
@Interceptor
@Threaded
public class ThreadedTimeoutInterceptor {

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        ThreadedScheduledService.INTERCEPTOR_THREAD.compareAndSet(null, Thread.currentThread());
        return context.proceed();
    }
}
