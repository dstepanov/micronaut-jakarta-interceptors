package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor declaring both kinds: the specification has {@code @AroundInvoke} interpose on business methods
 * and {@code @AroundTimeout} on the methods a timer service invokes.
 */
@Interceptor
@Timed
public class TimedInterceptor {

    static final String LOG = "TimedInterceptor";

    private final ScheduledRecords records;

    public TimedInterceptor(ScheduledRecords records) {
        this.records = records;
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        records.record(LOG, "aroundInvoke " + context.getMethod().getName());
        return context.proceed();
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        records.record(LOG, "aroundTimeout " + context.getMethod().getName() + " " + context.getTimer());
        return context.proceed();
    }
}
