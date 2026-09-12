package io.micronaut.interceptor.test.construct;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** The only interceptor of {@link AssociatedService} that takes part in its construction. */
@Interceptor
@Associated
@Priority(Interceptor.Priority.APPLICATION)
public class AssociatedConstructInterceptor {

    public AssociatedConstructInterceptor() {
        Stages.RECORDED.add("construct interceptor created");
    }

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Stages.RECORDED.add("construct interceptor before proceed");
        context.proceed();
        Stages.RECORDED.add("construct interceptor after proceed");
    }
}
