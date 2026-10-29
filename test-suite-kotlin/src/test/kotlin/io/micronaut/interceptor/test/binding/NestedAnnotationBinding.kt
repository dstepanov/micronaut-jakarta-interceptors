package io.micronaut.interceptor.test.binding

import jakarta.enterprise.util.Nonbinding
import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext

/**
 * An annotation held by the members of [Governed], with a member that does not bind. It is not a binding itself, so
 * the members excluded from it are read off its type, which a Kotlin annotation answers as properties.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target
annotation class Rule(val name: String, @get:Nonbinding val note: String = "")

/** A binding whose members are an annotation and an array of them. */
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Governed(val primary: Rule, val rules: Array<Rule> = [])

object GovernCalls {

    val recorded: MutableList<String> = mutableListOf()
}

@Interceptor
@Governed(primary = Rule("main", note = "of the interceptor"), rules = [Rule("first"), Rule("second", note = "x")])
open class GoverningInterceptor {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        GovernCalls.recorded += "governed ${context.method.name}"
        return context.proceed()
    }
}

@Singleton
open class GovernedService {

    /** Differs from the interceptor only in the members of [Rule] that do not bind. */
    @Governed(primary = Rule("main", note = "of the service"), rules = [Rule("first", note = "y"), Rule("second")])
    open fun work(): String = "work"

    /** Differs from the interceptor in a member of [Rule] that binds. */
    @Governed(primary = Rule("other"), rules = [Rule("first"), Rule("second")])
    open fun other(): String = "other"
}
