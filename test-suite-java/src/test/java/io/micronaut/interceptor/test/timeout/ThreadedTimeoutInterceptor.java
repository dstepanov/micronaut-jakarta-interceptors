package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Records the thread it runs on, immediately around proceeding to the timeout method. */
@Interceptor
@Threaded
public class ThreadedTimeoutInterceptor {

    private final ScheduledRecords records;

    public ThreadedTimeoutInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        records.firstThread(ThreadedScheduledService.INTERCEPTOR_THREAD);
        return context.proceed();
    }
}
