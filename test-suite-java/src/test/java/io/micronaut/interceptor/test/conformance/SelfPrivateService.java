package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Section 2.6 b) and cb): an around-invoke method may be declared on the target class itself, and it may be
 * private there as well.
 */
@Singleton
public class SelfPrivateService {

    public String work() {
        return "done";
    }

    @AroundInvoke
    private Object privateAround(InvocationContext context) throws Exception {
        Calls.RECORDED.add("private self aroundInvoke");
        return context.proceed();
    }
}
