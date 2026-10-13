package io.micronaut.interceptor.test.binding

import jakarta.enterprise.util.Nonbinding
import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext

/**
 * A binding with a member that does not bind and defaults to a value that is not empty. A Kotlin annotation answers
 * its members as properties rather than as methods.
 */
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class Graded(val level: String = "pass", @get:Nonbinding val note: String = "unremarked")

/** Carries [Graded], leaving the member that does not bind to its default. */
@Graded
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Reviewed

/** Carries [Graded] with a value of its own for the member that does not bind. */
@Graded(note = "remarked")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Annotated

object GradeCalls {

    val recorded: MutableList<String> = mutableListOf()
}

@Interceptor
@Graded
open class GradingInterceptor {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        GradeCalls.recorded += "graded ${context.method.name}"
        return context.proceed()
    }
}

/**
 * Carries [Graded] twice, through two annotations that differ only in the member that does not bind: one binding,
 * which is no conflict.
 */
@Singleton
open class GradedService {

    @Reviewed
    @Annotated
    open fun work(): String = "work"
}
