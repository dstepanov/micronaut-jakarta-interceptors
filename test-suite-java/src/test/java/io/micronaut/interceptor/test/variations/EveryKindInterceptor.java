package io.micronaut.interceptor.test.variations;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * One interceptor class declaring an interceptor method of every kind the specification defines. The same
 * interceptor instance serves all of them for one intercepted object, which the identity recorded below shows.
 */
@Interceptor
@Everything
public class EveryKindInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("aroundConstruct");
        context.proceed();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("postConstruct");
        context.proceed();
    }

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("aroundInvoke " + context.getMethod().getName());
        return context.proceed();
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        Calls.RECORDED.add("preDestroy");
        context.proceed();
    }
}
