package io.micronaut.interceptor.test

import io.micronaut.interceptor.MicronautConstructorInvocationContext
import io.micronaut.interceptor.MicronautMethodInvocationContext
import jakarta.annotation.PostConstruct
import jakarta.interceptor.AroundConstruct
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

/**
 * Reads the interception as Micronaut compiled it, and never calls getMethod(), getConstructor() or
 * getInterceptorBindings().
 */
@Interceptor
@Compiled(region = "users")
open class CompiledInterceptor {

    @AroundConstruct
    open fun construct(context: InvocationContext) {
        if (context is MicronautConstructorInvocationContext) {
            val constructor = context.beanConstructor
            // the same constructor, reached through the whole Micronaut construction
            val same = context.micronautInvocation.constructor === constructor
            Calls.recorded += "${context.interceptorKind} " +
                "${constructor.declaringBeanType.simpleName}(${constructor.arguments.size}) same=$same"
        }
        context.proceed()
    }

    @PostConstruct
    open fun created(context: InvocationContext) {
        record(context)
        context.proceed()
    }

    @AroundInvoke
    open fun invoke(context: InvocationContext): Any? {
        record(context)
        return context.proceed()
    }

    private fun record(context: InvocationContext) {
        if (context is MicronautMethodInvocationContext) {
            val method = context.executableMethod
            // the members of the binding, read without building the annotation
            val region = context.annotationMetadata.stringValue(Compiled::class.java, "region").orElse("none")
            // an argument by name, which the specification's own accessors have no way to reach
            val names = context.micronautInvocation.parameters.keys
            val parameters = if (names.isEmpty()) "" else " parameters=$names"
            Calls.recorded += "${context.interceptorKind} " +
                "${method.declaringType.simpleName}.${method.methodName} region=$region$parameters"
        }
    }
}
