package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

/**
 * A timeout interceptor reached through {@code @Interceptors} rather than through a binding annotation.
 */
public class NamedTimeoutInterceptor {

    static final String LOG = "NamedTimeoutInterceptor";

    private final ScheduledRecords records;

    public NamedTimeoutInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        records.record(LOG, "named aroundTimeout " + context.getMethod().getName());
        return context.proceed();
    }
}
