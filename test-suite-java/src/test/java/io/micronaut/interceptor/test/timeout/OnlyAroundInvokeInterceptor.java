package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor declaring no {@code @AroundTimeout} method still interposes on a scheduled method, so that
 * scheduling a method does not quietly take its interception away.
 */
@Interceptor
@Watched
public class OnlyAroundInvokeInterceptor {

    static final String LOG = "OnlyAroundInvokeInterceptor";

    private final ScheduledRecords records;

    public OnlyAroundInvokeInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        records.record(LOG, "aroundInvoke " + context.getMethod().getName());
        return context.proceed();
    }
}
