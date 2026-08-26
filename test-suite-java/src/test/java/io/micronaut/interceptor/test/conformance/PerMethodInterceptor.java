package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * Sections 2.9 b), d) and e): bound to two business methods only, so its around-construct and post-construct
 * methods are never invoked, and one instance serves the whole target instance.
 */
public class PerMethodInterceptor {

    private int seen;

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        seen++;
        Calls.RECORDED.add("method interceptor " + context.getMethod().getName() + " seen=" + seen
            + " instance=" + System.identityHashCode(this));
        return context.proceed();
    }

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("MUST NOT construct");
        context.proceed();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("MUST NOT postConstruct");
        context.proceed();
    }
}
