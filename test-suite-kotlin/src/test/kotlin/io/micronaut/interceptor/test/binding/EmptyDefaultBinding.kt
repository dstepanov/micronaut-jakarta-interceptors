package io.micronaut.interceptor.test.binding

import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext

/**
 * A binding whose members default to empty values: an empty string, which Micronaut leaves out of the defaults it
 * records, and an empty array, which it keeps.
 */
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Shelved(val value: String = "", val labels: Array<String> = [])

object ShelfCalls {

    val recorded: MutableList<String> = mutableListOf()
}

@Interceptor
@Shelved
open class UnnamedShelfInterceptor {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        ShelfCalls.recorded += "unnamed shelf"
        return context.proceed()
    }
}

@Singleton
open class ShelvedService {

    @Shelved
    open fun leftToItsDefaults(): String = "defaults"

    @Shelved(value = "", labels = [])
    open fun everyDefaultSpeltOut(): String = "every default"

    @Shelved("top")
    open fun onANamedShelf(): String = "named"
}
