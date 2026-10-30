package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

/** Named by a scheduled method rather than by its class. */
public class MethodNamedTimeoutInterceptor {

    static final String LOG = "MethodNamedTimeoutInterceptor";

    private final ScheduledRecords records;

    public MethodNamedTimeoutInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        records.record(LOG, context.getMethod().getName());
        return context.proceed();
    }
}
