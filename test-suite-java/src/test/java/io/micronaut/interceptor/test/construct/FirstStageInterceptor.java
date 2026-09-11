package io.micronaut.interceptor.test.construct;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Staged
@Priority(Interceptor.Priority.APPLICATION)
public class FirstStageInterceptor {

    public FirstStageInterceptor() {
        Stages.RECORDED.add("first interceptor created");
    }

    @Inject
    void inject(Stages stages) {
        Stages.RECORDED.add("first interceptor injected");
    }

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Stages.RECORDED.add("first before proceed");
        context.proceed();
        Stages.RECORDED.add("first after proceed");
    }
}
