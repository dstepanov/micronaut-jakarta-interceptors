package io.micronaut.interceptor.test.binding

import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext

/** A binding that other annotations carry to what they are declared on. */
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class Watched

/** Not a binding annotation itself: it only carries [Watched]. */
@Watched
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Surveilled

object WatchCalls {

    val recorded: MutableList<String> = mutableListOf()
}

@Interceptor
@Watched
open class WatchingInterceptor {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        WatchCalls.recorded += "watched ${context.method.name}"
        return context.proceed()
    }
}

/** The plain carrier on a function, which is all the interception the class declares. */
@Singleton
open class SurveilledService {

    @Surveilled
    open fun work(): String = "work"

    open fun rest(): String = "rest"
}
