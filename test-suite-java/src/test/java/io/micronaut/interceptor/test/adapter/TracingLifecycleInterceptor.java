package io.micronaut.interceptor.test.adapter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor of the whole life of the bean it is bound to, which reads the object it is interposing on as the
 * type of that bean.
 */
@Interceptor
@Traced
public class TracingLifecycleInterceptor {

    public static int created;

    public TracingLifecycleInterceptor() {
        created++;
    }

    @AroundConstruct
    public void aroundConstruct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("construct");
        context.proceed();
    }

    @PostConstruct
    public void postConstruct(InvocationContext context) throws Exception {
        // the specification hands an interceptor of this bean the bean itself, of the type that declared the binding
        AdaptedService target = (AdaptedService) context.getTarget();
        Calls.RECORDED.add("post construct " + (target == null ? "nothing" : "the bean"));
        context.proceed();
    }

    @PreDestroy
    public void preDestroy(InvocationContext context) throws Exception {
        Calls.RECORDED.add("pre destroy");
        context.proceed();
    }

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("invoke " + context.getMethod().getName());
        return context.proceed();
    }
}
