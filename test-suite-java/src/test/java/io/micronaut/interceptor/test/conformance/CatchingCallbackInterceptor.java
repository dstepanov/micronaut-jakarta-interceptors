package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.7.1 b): a lifecycle callback interceptor method may catch what another one in the chain threw and
 * clean up before returning.
 */
@Interceptor
@Fragile
@Priority(Interceptor.Priority.APPLICATION - 10)
public class CatchingCallbackInterceptor {

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        try {
            context.proceed();
        } catch (RuntimeException e) {
            Calls.RECORDED.add("caught " + e.getMessage());
        }
    }
}
