
package io.micronaut.interceptor.test.construct;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.io.IOException;

/**
 * An {@code @AroundConstruct} method may throw a checked exception, unlike a lifecycle callback interceptor method.
 */
@Interceptor
@Fragile
public class FailingConstructInterceptor {

    @AroundConstruct
    public void refuse(InvocationContext context) throws IOException {
        throw new IOException("refused before construction");
    }
}
