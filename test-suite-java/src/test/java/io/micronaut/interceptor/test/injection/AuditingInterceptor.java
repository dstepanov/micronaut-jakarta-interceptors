package io.micronaut.interceptor.test.injection;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor class is a bean, so it has its dependencies injected like any other.
 */
@Interceptor
@Audited
public class AuditingInterceptor {

    private final AuditLog log;

    public AuditingInterceptor(AuditLog log) {
        this.log = log;
    }

    @AroundInvoke
    public Object audit(InvocationContext context) throws Exception {
        log.record(context.getMethod().getName());
        return context.proceed();
    }
}
