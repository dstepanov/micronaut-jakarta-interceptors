package io.micronaut.interceptor.test.construct;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Throws whatever the test asks it to instead of proceeding, and otherwise proceeds.
 */
@Interceptor
@FragileBuilt
public class RefusingConstructInterceptor {

    static volatile Exception refusal;
    static volatile Exception seen;

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        if (refusal != null) {
            throw refusal;
        }
        try {
            context.proceed();
        } catch (Exception e) {
            seen = e;
            throw e;
        }
    }
}
