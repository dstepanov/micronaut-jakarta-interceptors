package io.micronaut.interceptor.test.construct;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Bound to {@link AssociatedService} and declaring no {@code @AroundConstruct} method, so it takes no part in the
 * construction of that object. It is still one of the interceptor classes associated with it, and section 2.3 da) has
 * injection completed on its instance before any around-construct method runs.
 */
@Interceptor
@Associated
@Priority(Interceptor.Priority.APPLICATION + 1)
public class AssociatedInvokeInterceptor {

    public AssociatedInvokeInterceptor() {
        Stages.RECORDED.add("invoke interceptor created");
    }

    @Inject
    void inject(Stages stages) {
        Stages.RECORDED.add("invoke interceptor injected");
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        Stages.RECORDED.add("invoke interceptor around");
        return context.proceed();
    }
}
