package io.micronaut.interceptor.test

import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.InvocationContext

/**
 * A plain class named directly by `@Interceptors`, without `@Interceptor`.
 */
open class CountingInterceptor {

    @AroundInvoke
    open fun count(context: InvocationContext): Any? {
        Calls.recorded += "count"
        return context.proceed()
    }
}
