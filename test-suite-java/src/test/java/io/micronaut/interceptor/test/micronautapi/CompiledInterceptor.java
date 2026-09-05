package io.micronaut.interceptor.test.micronautapi;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.interceptor.MicronautConstructorInvocationContext;
import io.micronaut.interceptor.MicronautInvocationContext;
import io.micronaut.interceptor.MicronautMethodInvocationContext;
import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Reads everything it needs through the Micronaut view of the context, and never calls {@code getMethod()},
 * {@code getConstructor()} or {@code getInterceptorBindings()}.
 */
@Interceptor
@Compiled(region = "users")
public class CompiledInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        if (context instanceof MicronautConstructorInvocationContext micronaut) {
            Recorded.VALUES.add(micronaut.getInterceptorKind() + " "
                + micronaut.getBeanConstructor().getDeclaringBeanType().getSimpleName()
                + "(" + micronaut.getBeanConstructor().getArguments().length + ")"
                // the same constructor reached through the whole Micronaut construction
                + " same=" + (micronaut.getMicronautInvocation().getConstructor()
                    == micronaut.getBeanConstructor()));
        }
        context.proceed();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        record(context);
        context.proceed();
    }

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        record(context);
        return context.proceed();
    }

    private static void record(InvocationContext context) {
        if (context instanceof MicronautMethodInvocationContext micronaut) {
            Recorded.VALUES.add(micronaut.getInterceptorKind() + " "
                + micronaut.getExecutableMethod().getDeclaringType().getSimpleName() + "."
                + micronaut.getExecutableMethod().getMethodName()
                // the members of the binding, read without building the annotation
                + " region=" + region(micronaut)
                // the whole Micronaut invocation, for what the other accessors do not reach
                + named(micronaut.getMicronautInvocation()));
        }
    }

    /** An argument read by name, which the specification's own accessors have no way to do. */
    private static String named(MethodInvocationContext<Object, Object> invocation) {
        if (invocation.getParameters().isEmpty()) {
            return "";
        }
        return " parameters=" + invocation.getParameters().keySet();
    }

    private static String region(MicronautInvocationContext context) {
        return context.getAnnotationMetadata().stringValue(Compiled.class, "region").orElse("none");
    }
}
