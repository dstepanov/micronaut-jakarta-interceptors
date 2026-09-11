package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Layered
public class LayerInterceptor extends BaseLayerInterceptor {

    @PostConstruct
    private void postConstruct(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("own post");
        context.proceed();
    }

    @AroundInvoke
    private Object around(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("own around");
        return context.proceed();
    }
}
