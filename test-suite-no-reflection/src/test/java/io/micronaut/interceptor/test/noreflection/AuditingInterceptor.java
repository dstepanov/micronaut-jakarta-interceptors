package io.micronaut.interceptor.test.noreflection;

import io.micronaut.interceptor.MicronautConstructorInvocationContext;
import io.micronaut.interceptor.MicronautInvocationContext;
import io.micronaut.interceptor.MicronautMethodInvocationContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Interposes on every kind of interception, and asks each context both ways: through the accessors of the
 * specification that return an object of the Java reflection API, and through the ones of
 * {@link MicronautInvocationContext}, which reflect on nothing.
 */
@Audited
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditingInterceptor {

    @AroundConstruct
    void construct(InvocationContext context) throws Exception {
        Probe.CALLS.add("aroundConstruct");
        Probe.attempt("getConstructor()", context::getConstructor);
        MicronautConstructorInvocationContext micronaut = (MicronautConstructorInvocationContext) context;
        Probe.attempt("constructor kind", micronaut::getInterceptorKind);
        Probe.attempt("constructor type", () -> micronaut.getBeanConstructor().getDeclaringBeanType());
        Probe.attempt("constructor arguments", () -> micronaut.getBeanConstructor().getArguments().length);
        Probe.attempt("constructor binding", () -> micronaut.getAnnotationMetadata().hasAnnotation(Audited.class));
        context.proceed();
    }

    @PostConstruct
    void created(InvocationContext context) throws Exception {
        Probe.CALLS.add("postConstruct");
        Probe.attempt("postConstruct getMethod()", context::getMethod);
        MicronautMethodInvocationContext micronaut = (MicronautMethodInvocationContext) context;
        Probe.attempt("postConstruct kind", micronaut::getInterceptorKind);
        Probe.attempt("postConstruct method", () -> micronaut.getExecutableMethod().getMethodName());
        context.proceed();
    }

    @PreDestroy
    void destroyed(InvocationContext context) throws Exception {
        Probe.CALLS.add("preDestroy");
        context.proceed();
    }

    /**
     * Private, so that generated code cannot call it: Micronaut reaches it reflectively, which the
     * {@code @ReflectiveAccess} the processor declares on it permits. That needs no reflection module.
     */
    @AroundInvoke
    private Object invoke(InvocationContext context) throws Exception {
        Probe.CALLS.add("aroundInvoke");
        Probe.attempt("getMethod()", context::getMethod);
        Probe.attempt("getInterceptorBindings()", context::getInterceptorBindings);
        Probe.attempt("getInterceptorBinding(Class)", () -> context.getInterceptorBinding(Audited.class));
        Probe.attempt("getInterceptorBindings(Class)", () -> context.getInterceptorBindings(Audited.class));
        // not a binding of the element, which the annotation metadata answers without building anything
        Probe.attempt("getInterceptorBinding(Class) of no binding", () -> context.getInterceptorBinding(Deprecated.class));
        Probe.attempt("getInterceptorBindings(Class) of no binding", () -> context.getInterceptorBindings(Deprecated.class));
        MicronautMethodInvocationContext micronaut = (MicronautMethodInvocationContext) context;
        Probe.attempt("method kind", micronaut::getInterceptorKind);
        Probe.attempt("method", () -> micronaut.getExecutableMethod().getMethodName());
        Probe.attempt("method declaring type", () -> micronaut.getExecutableMethod().getDeclaringType());
        Probe.attempt("method binding", () -> micronaut.getAnnotationMetadata().hasAnnotation(Audited.class));
        Probe.attempt("parameter", () -> context.getParameters()[0]);
        context.setParameters(new Object[]{"replaced"});
        return context.proceed();
    }
}
