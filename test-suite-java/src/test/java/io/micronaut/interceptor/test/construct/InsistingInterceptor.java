
package io.micronaut.interceptor.test.construct;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Proceeds twice, as an interceptor may to run the rest of the chain again. The constructor still runs once.
 */
@Interceptor
@Insistent
public class InsistingInterceptor {

    @AroundConstruct
    public void insist(InvocationContext context) throws Exception {
        context.proceed();
        context.proceed();
    }
}
