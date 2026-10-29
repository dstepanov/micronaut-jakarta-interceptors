package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Section 2.8 c): an around-timeout method may be protected. */
@Interceptor
@Varied
public class ProtectedTimeoutInterceptor {

    private final ScheduledRecords records;

    public ProtectedTimeoutInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundTimeout
    protected Object aroundTimeout(InvocationContext context) throws Exception {
        records.record(VariedCalls.LOG, "protected");
        return context.proceed();
    }
}
