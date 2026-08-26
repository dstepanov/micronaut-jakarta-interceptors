package io.micronaut.interceptor.test

import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.InvocationContext

/**
 * A plain class named directly by {@code @Interceptors}, without {@code @Interceptor}.
 */
class CountingInterceptor {

    @AroundInvoke
    Object count(InvocationContext context) throws Exception {
        Calls.RECORDED << "count"
        return context.proceed()
    }
}
