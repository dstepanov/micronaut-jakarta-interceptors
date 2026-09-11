package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Interposes on the post-construct callback of an object whose callback throws. */
@Interceptor
@Unready
public class UnreadyInterceptor {

    public UnreadyInterceptor() {
        Destructions.RECORDED.add("post-construct interceptor created");
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("post-construct interceptor destroyed");
    }
}
