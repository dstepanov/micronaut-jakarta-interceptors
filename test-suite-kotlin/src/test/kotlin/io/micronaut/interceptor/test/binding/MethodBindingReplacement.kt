package io.micronaut.interceptor.test.binding

import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext

/** A binding with a default for its member, which a method may declare without giving the member a value. */
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Zone(val value: String = "z")

object ZoneCalls {

    val recorded: MutableList<String> = mutableListOf()
}

@Interceptor
@Zone("a")
open class ZoneAInterceptor {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        ZoneCalls.recorded += "zone a"
        return context.proceed()
    }
}

@Interceptor
@Zone
open class DefaultZoneInterceptor {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        ZoneCalls.recorded += "zone z"
        return context.proceed()
    }
}

@Singleton
@Zone("a")
open class ZonedService {

    open fun inherited(): String = "inherited"

    /** Replaces the class's `@Zone("a")` with `@Zone("z")`, the default of the member. */
    @Zone
    open fun replacedByDefault(): String = "by default"
}
