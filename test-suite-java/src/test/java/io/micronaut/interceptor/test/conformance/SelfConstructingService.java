package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

/**
 * A bean that declares an {@code @AroundConstruct} method on itself, which section 2.7 d) says it should not: that
 * callback belongs to an interceptor class, and there is no instance yet to invoke it on while the bean is being
 * constructed.
 */
@Singleton
public class SelfConstructingService {

    public SelfConstructingService() {
        Calls.RECORDED.add("constructed");
    }

    @AroundConstruct
    public void around(InvocationContext context) throws Exception {
        Calls.RECORDED.add("aroundConstruct on itself");
        context.proceed();
    }

    public String work() {
        return "done";
    }
}
