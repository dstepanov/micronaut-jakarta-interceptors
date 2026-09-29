package io.micronaut.interceptor.test.named;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor class that does not declare {@code @Interceptor}, and whose only interceptor methods interpose on
 * the lifecycle callbacks of what it intercepts.
 *
 * <p>It is an interceptor class only because {@code @Interceptors} names it. Apart from the two callbacks, it carries
 * none of the annotations that bring a class to the attention of the bean processor, so it has to be those two that
 * do, or it is never made the bean the class naming it is intercepted with.</p>
 */
public class UndeclaredLifecycleOnlyInterceptor {

    @PostConstruct
    void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("undeclared postConstruct " + targetOf(context));
        context.proceed();
    }

    @PreDestroy
    void destroyed(InvocationContext context) throws Exception {
        Calls.RECORDED.add("undeclared preDestroy " + targetOf(context));
        context.proceed();
    }

    private static String targetOf(InvocationContext context) {
        return context.getTarget() instanceof UndeclaredLifecycleOnlyNamedService ? "of the service" : "of something else";
    }
}
