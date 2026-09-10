package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Section 2.8 c): an around-timeout method may have package access. */
@Interceptor
@Varied
public class PackagePrivateTimeoutInterceptor {

    @AroundTimeout
    Object aroundTimeout(InvocationContext context) throws Exception {
        VariedCalls.RECORDED.add("package-private");
        return context.proceed();
    }
}
