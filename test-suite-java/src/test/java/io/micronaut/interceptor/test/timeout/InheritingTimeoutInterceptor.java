package io.micronaut.interceptor.test.timeout;

import io.micronaut.interceptor.test.ScheduledRecords;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.8 ba): an around-timeout method may be inherited, and the one its superclass declares is invoked
 * before the one it declares itself.
 */
@Interceptor
@Varied
public class InheritingTimeoutInterceptor extends BaseTimeoutInterceptor {

    public InheritingTimeoutInterceptor(ScheduledRecords records) {
        super(records);
    }

    @AroundTimeout
    public Object declaredAroundTimeout(InvocationContext context) throws Exception {
        records.record(VariedCalls.LOG, "declared");
        return context.proceed();
    }
}
