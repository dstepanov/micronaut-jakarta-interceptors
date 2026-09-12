package io.micronaut.interceptor.test.named;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor class whose only interceptor methods interpose on the lifecycle callbacks of what it intercepts.
 *
 * <p>The annotation that would make Micronaut invoke such a method as a callback of this class is taken off it once
 * the processor has recorded it, so nothing about the class says afterwards that it is an interceptor class. What it
 * declared is recorded on it instead, and that is what the class naming it reads, however the two were ordered in
 * the compilation.</p>
 */
@Interceptor
public class LifecycleOnlyInterceptor {

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("lifecycle only postConstruct");
        context.proceed();
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        Calls.RECORDED.add("lifecycle only preDestroy");
        context.proceed();
    }
}
