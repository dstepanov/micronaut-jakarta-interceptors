package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.concurrent.atomic.AtomicInteger;

/** Interposes on the lifecycle of a bean a factory produced, and has a lifecycle of its own. */
@Interceptor
@Produced
public class ProducedLifecycleInterceptor {

    private static final AtomicInteger CREATED = new AtomicInteger();

    private final int number = CREATED.incrementAndGet();

    public static void reset() {
        CREATED.set(0);
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Destructions.RECORDED.add("post-construct on interceptor " + number);
        context.proceed();
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        Destructions.RECORDED.add("pre-destroy on interceptor " + number);
        context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("interceptor " + number + " destroyed");
    }
}
