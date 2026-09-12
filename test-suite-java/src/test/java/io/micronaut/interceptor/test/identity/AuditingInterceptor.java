package io.micronaut.interceptor.test.identity;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Audited
public class AuditingInterceptor {

    @AroundInvoke
    public Object audit(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
