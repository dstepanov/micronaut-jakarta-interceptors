package io.micronaut.interceptor.test.variations;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * The same four kinds, reached through {@code @Interceptors} rather than through a binding annotation, and by a
 * plain class that is not annotated {@code @Interceptor}.
 */
public class NamedEveryKindInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("named aroundConstruct");
        context.proceed();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("named postConstruct");
        context.proceed();
    }

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("named aroundInvoke " + context.getMethod().getName());
        return context.proceed();
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        Calls.RECORDED.add("named preDestroy");
        context.proceed();
    }
}
