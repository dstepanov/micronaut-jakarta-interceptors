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
class CompiledInterceptor {

    @AroundConstruct
    void construct(InvocationContext context) throws Exception {
        if (context instanceof MicronautConstructorInvocationContext) {
            String kind = context.interceptorKind
            String type = context.beanConstructor.declaringBeanType.simpleName
            int arguments = context.beanConstructor.arguments.length
            // the same constructor, reached through the whole Micronaut construction
            boolean same = context.micronautInvocation.constructor.is(context.beanConstructor)
            Calls.RECORDED.add("$kind $type($arguments) same=$same".toString())
        }
        context.proceed()
    }

    @PostConstruct
    void created(InvocationContext context) throws Exception {
        record(context)
        context.proceed()
    }

    @AroundInvoke
    Object invoke(InvocationContext context) throws Exception {
        record(context)
        return context.proceed()
    }

    private static void record(InvocationContext context) {
        if (context instanceof MicronautMethodInvocationContext) {
            String kind = context.interceptorKind
            String method = "${context.executableMethod.declaringType.simpleName}.${context.executableMethod.methodName}"
            // the members of the binding, read without building the annotation
            String region = context.annotationMetadata.stringValue(Compiled, "region").orElse("none")
            // an argument by name, which the specification's own accessors have no way to reach
            Set<String> names = context.micronautInvocation.parameters.keySet()
            String parameters = names.isEmpty() ? "" : " parameters=$names"
            Calls.RECORDED.add("$kind $method region=$region$parameters".toString())
        }
    }
}
