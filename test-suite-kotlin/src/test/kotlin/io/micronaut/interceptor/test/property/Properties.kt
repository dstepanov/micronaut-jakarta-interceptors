package io.micronaut.interceptor.test.property

import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext

/** A binding declared on a property accessor, which is a function as far as the specification is concerned. */
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Guarded

object Calls {

    val recorded: MutableList<String> = mutableListOf()

    fun clear() = recorded.clear()
}

@Interceptor
@Guarded
open class GuardingInterceptor {

    @AroundInvoke
    open fun guard(context: InvocationContext): Any? {
        Calls.recorded += "guarded ${context.method.name}"
        return context.proceed()
    }
}

/** A bean whose accessors carry the binding themselves, the class declaring none. */
@Singleton
open class AccessorBoundService {

    @get:Guarded
    open val read: String
        get() {
            Calls.recorded += "read"
            return "read"
        }

    @get:Guarded
    @set:Guarded
    open var written: String = "written"
        get() {
            Calls.recorded += "get written"
            return field
        }
        set(value) {
            Calls.recorded += "set written"
            field = value
        }

    open val plain: String
        get() {
            Calls.recorded += "plain"
            return "plain"
        }
}

/** A bean bound at class level, whose properties are business methods of it as its functions are. */
@Singleton
@Guarded
open class ClassBoundService {

    open val read: String
        get() {
            Calls.recorded += "read"
            return "read"
        }

    open var written: String = "written"
        get() {
            Calls.recorded += "get written"
            return field
        }
        set(value) {
            Calls.recorded += "set written"
            field = value
        }

    open fun work(): String {
        Calls.recorded += "work"
        return "work"
    }
}

/**
 * A bean bound at class level that declares a property and a function without `open`. Each is final on the virtual
 * machine unless the all-open compiler plugin opens it, so neither can be intercepted and neither keeps the class
 * from compiling: what the class declares `open` is intercepted and the rest is left alone.
 */
@Singleton
@Guarded
open class PartlyOpenService {

    val fixed: String
        get() {
            Calls.recorded += "fixed"
            return "fixed"
        }

    open fun work(): String {
        Calls.recorded += "work"
        return "work"
    }
}
