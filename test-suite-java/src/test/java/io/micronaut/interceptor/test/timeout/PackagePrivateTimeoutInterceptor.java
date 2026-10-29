package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Section 2.8 c): an around-timeout method may have package access. */
@Interceptor
@Varied
public class PackagePrivateTimeoutInterceptor {

    private final ScheduledRecords records;

    public PackagePrivateTimeoutInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundTimeout
    Object aroundTimeout(InvocationContext context) throws Exception {
        records.record(VariedCalls.LOG, "package-private");
        return context.proceed();
    }
}
