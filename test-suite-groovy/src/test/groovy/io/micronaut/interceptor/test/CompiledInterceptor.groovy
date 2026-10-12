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
            def construction = context.micronautInvocation
            String kind = construction.kind
            String type = construction.constructor.declaringBeanType.simpleName
            int arguments = construction.constructor.arguments.length
            // the metadata of the constructor, which the construction itself does not carry
            String region = context.annotationMetadata.stringValue(Compiled, "region").orElse("none")
            Calls.RECORDED.add("$kind $type($arguments) region=$region".toString())
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
            def invocation = context.micronautInvocation
            String kind = invocation.kind
            String method = "${invocation.executableMethod.declaringType.simpleName}.${invocation.executableMethod.methodName}"
            // the members of the binding, read without building the annotation
            String region = context.annotationMetadata.stringValue(Compiled, "region").orElse("none")
            // an argument by name, which the specification's own accessors have no way to reach
            Set<String> names = invocation.parameters.keySet()
            String parameters = names.isEmpty() ? "" : " parameters=$names"
            Calls.RECORDED.add("$kind $method region=$region$parameters".toString())
        }
    }
}
