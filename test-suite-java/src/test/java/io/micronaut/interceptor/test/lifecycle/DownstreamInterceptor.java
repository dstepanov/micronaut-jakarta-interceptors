package io.micronaut.interceptor.test.lifecycle;

import jakarta.annotation.Priority;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Ordered after the halting one, so that it runs only if the chain was proceeded. */
@Interceptor
@Halted
@Priority(Interceptor.Priority.APPLICATION)
public class DownstreamInterceptor {

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("downstream postConstruct");
        context.proceed();
    }

    @PreDestroy
    public void destroyed(InvocationContext context) throws Exception {
        Calls.RECORDED.add("downstream preDestroy");
        context.proceed();
    }
}
