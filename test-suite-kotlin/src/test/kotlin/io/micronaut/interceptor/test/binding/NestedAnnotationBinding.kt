package io.micronaut.interceptor.test.binding

import jakarta.enterprise.util.Nonbinding
import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext

/**
 * The annotation the single member of [Governed] holds, with a member that does not bind. It is not a binding
 * itself, so the members excluded from it are read off its type, which a Kotlin annotation answers as properties.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target
annotation class Rule(val name: String, @get:Nonbinding val note: String = "")

/**
 * The annotation held by a member of [Clause] alone, which is reached only through the array of [Governed].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target
annotation class Detail(val level: Int = 0)

/**
 * The annotation the array member of [Governed] holds, used nowhere else, with a member that does not bind and one
 * holding a further annotation.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target
annotation class Clause(val name: String, @get:Nonbinding val note: String = "", val detail: Detail = Detail())

/** A binding whose members are an annotation and an array of another. */
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Governed(val primary: Rule, val clauses: Array<Clause> = [])

object GovernCalls {

    val recorded: MutableList<String> = mutableListOf()
}

@Interceptor
@Governed(
    primary = Rule("main", note = "of the interceptor"),
    clauses = [Clause("first"), Clause("second", note = "x", detail = Detail(1))]
)
open class GoverningInterceptor {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        GovernCalls.recorded += "governed ${context.method.name}"
        return context.proceed()
    }
}

@Singleton
open class GovernedService {

    /** Differs from the interceptor only in the members that do not bind, in the single member and in the array. */
    @Governed(
        primary = Rule("main", note = "of the service"),
        clauses = [Clause("first", note = "y"), Clause("second", detail = Detail(1))]
    )
    open fun work(): String = "work"

    /** Differs from the interceptor in a member of [Rule] that binds. */
    @Governed(primary = Rule("other"), clauses = [Clause("first"), Clause("second", detail = Detail(1))])
    open fun other(): String = "other"

    /** Agrees with the interceptor in the single member, and differs in a member of an element of the array. */
    @Governed(primary = Rule("main"), clauses = [Clause("first"), Clause("third", detail = Detail(1))])
    open fun otherClause(): String = "other clause"

    /** Differs from the interceptor only in the annotation an element of the array holds. */
    @Governed(primary = Rule("main"), clauses = [Clause("first"), Clause("second", detail = Detail(2))])
    open fun otherDetail(): String = "other detail"
}
