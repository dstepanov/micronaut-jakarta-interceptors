package io.micronaut.interceptor.test.construct;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Staged
@Priority(Interceptor.Priority.APPLICATION + 1)
public class SecondStageInterceptor {

    public SecondStageInterceptor() {
        Stages.RECORDED.add("second interceptor created");
    }

    @Inject
    void inject(Stages stages) {
        Stages.RECORDED.add("second interceptor injected");
    }

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Stages.RECORDED.add("second before proceed");
        context.proceed();
        Stages.RECORDED.add("second after proceed");
    }
}
