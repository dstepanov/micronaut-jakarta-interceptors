package io.micronaut.interceptor.test.graalvm;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.annotation.Annotation;

/**
 * Declares its interceptor methods private, which the specification allows and which Micronaut reaches through a
 * reflective executable method, and asks the context for the elements the specification describes with the
 * reflection of the platform: the method, the constructor and the binding annotations.
 */
@Interceptor
@Guarded
public class GuardingInterceptor {

    @AroundConstruct
    private void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("aroundConstruct " + context.getConstructor().getDeclaringClass().getSimpleName());
        context.proceed();
    }

    @PostConstruct
    private void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("postConstruct " + name(context.getMethod() == null ? null : context.getMethod().getName()));
        context.proceed();
    }

    @AroundInvoke
    private Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("aroundInvoke " + context.getMethod().getName());
        for (Annotation binding : context.getInterceptorBindings()) {
            Calls.RECORDED.add("binding " + binding.annotationType().getSimpleName());
        }
        return context.proceed();
    }

    private static String name(String value) {
        return value == null ? "none" : value;
    }
}
