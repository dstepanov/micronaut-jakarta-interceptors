package io.micronaut.interceptor.test.factory;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor class declaring {@code @Interceptor} and nothing that makes it a bean of its own, which
 * {@link AuditingInterceptorFactory} produces as well.
 */
@Interceptor
@Audited
public class AuditingInterceptor {

    private final String source;

    public AuditingInterceptor() {
        this("class");
    }

    AuditingInterceptor(String source) {
        this.source = source;
    }

    public String source() {
        return source;
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("audited by " + source);
        return context.proceed();
    }
}
