package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Interposes on the construction of an object whose constructor throws. */
@Interceptor
@Doomed
public class DoomedConstructInterceptor {

    public DoomedConstructInterceptor() {
        Destructions.RECORDED.add("construct interceptor created");
    }

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("construct interceptor destroyed");
    }
}
