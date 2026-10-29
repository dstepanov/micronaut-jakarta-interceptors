package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

/** Declares an around-timeout method that an interceptor class inherits. */
public abstract class BaseTimeoutInterceptor {

    protected final ScheduledRecords records;

    protected BaseTimeoutInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundTimeout
    public Object inheritedAroundTimeout(InvocationContext context) throws Exception {
        records.record(VariedCalls.LOG, "inherited");
        return context.proceed();
    }
}
