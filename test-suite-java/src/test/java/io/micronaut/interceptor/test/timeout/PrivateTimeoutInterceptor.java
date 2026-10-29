package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.8 c): an around-timeout method may have private access.
 */
@Interceptor
@PrivatelyTimed
public class PrivateTimeoutInterceptor {

    static final String LOG = "PrivateTimeoutInterceptor";

    private final ScheduledRecords records;

    public PrivateTimeoutInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundTimeout
    private Object aroundTimeout(InvocationContext context) throws Exception {
        records.record(LOG, "private aroundTimeout " + context.getMethod().getName());
        return context.proceed();
    }
}
