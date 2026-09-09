package io.micronaut.interceptor.test.lifecycle;

import jakarta.annotation.Priority;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Returns without proceeding, which is what the rest of the event must not survive. */
@Interceptor
@Halted
@Priority(Interceptor.Priority.APPLICATION - 10)
public class HaltingInterceptor {

    @PostConstruct
    public void created(InvocationContext context) {
        Calls.RECORDED.add("halting postConstruct");
    }

    @PreDestroy
    public void destroyed(InvocationContext context) {
        Calls.RECORDED.add("halting preDestroy");
    }
}
