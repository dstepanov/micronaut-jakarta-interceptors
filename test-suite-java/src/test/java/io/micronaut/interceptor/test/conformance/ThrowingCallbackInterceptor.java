package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.7.1 a): a lifecycle callback interceptor method may throw a runtime exception. What it must not throw
 * is a checked exception, and this one throws whichever the test asks for.
 */
@Interceptor
@Fragile
@Priority(Interceptor.Priority.APPLICATION)
public class ThrowingCallbackInterceptor {

    static RuntimeException runtimeFailure;
    static Exception checkedFailure;

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("throwing callback");
        if (runtimeFailure != null) {
            throw runtimeFailure;
        }
        if (checkedFailure != null) {
            throw checkedFailure;
        }
        context.proceed();
    }
}
