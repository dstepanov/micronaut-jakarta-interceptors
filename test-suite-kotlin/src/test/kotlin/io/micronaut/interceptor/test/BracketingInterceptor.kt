package io.micronaut.interceptor.test

import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

/**
 * Records what an interceptor written the ordinary way sees of a suspending function: the part before `proceed()`,
 * what `proceed()` returned, an exception it threw, and the `finally` after it.
 */
@Interceptor
@Bracketed
open class BracketingInterceptor {

    @AroundInvoke
    open fun bracket(context: InvocationContext): Any? {
        Calls.recorded += "interceptor before"
        try {
            val result = context.proceed()
            Calls.recorded += if (result === COROUTINE_SUSPENDED) "interceptor returned COROUTINE_SUSPENDED"
                else "interceptor returned $result"
            return result
        } catch (e: Exception) {
            Calls.recorded += "interceptor caught ${e.message}"
            throw e
        } finally {
            Calls.recorded += "interceptor finished"
        }
    }
}
