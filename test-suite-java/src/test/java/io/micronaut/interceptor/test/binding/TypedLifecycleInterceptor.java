package io.micronaut.interceptor.test.binding;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.Set;
import java.util.TreeSet;

/**
 * Reads a repeated binding by type from each kind of context, and the singular accessor both before the whole set
 * has been built and after, which are two different paths through the context.
 */
@Interceptor
@TypedLifecycle
public class TypedLifecycleInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        record("aroundConstruct", context);
        context.proceed();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        record("postConstruct", context);
        context.proceed();
    }

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        record("aroundInvoke", context);
        return context.proceed();
    }

    private static void record(String kind, InvocationContext context) {
        Labelled cold = context.getInterceptorBinding(Labelled.class);
        Set<String> plural = new TreeSet<>();
        for (Labelled labelled : context.getInterceptorBindings(Labelled.class)) {
            plural.add(labelled.value());
        }
        // building the whole set takes the other path on the next call
        context.getInterceptorBindings();
        Labelled warm = context.getInterceptorBinding(Labelled.class);
        Cached cached = context.getInterceptorBinding(Cached.class);
        TypedLifecycleCalls.RECORDED.add(kind
            + " plural=" + plural
            + " cold=" + (cold == null ? "null" : cold.value())
            + " warm=" + (warm == null ? "null" : warm.value())
            + " cached=" + (cached == null ? "null" : cached.region()));
    }
}
