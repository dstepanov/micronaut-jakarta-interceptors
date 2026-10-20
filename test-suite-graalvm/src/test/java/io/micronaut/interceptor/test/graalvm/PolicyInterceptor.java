package io.micronaut.interceptor.test.graalvm;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the annotations the members of its binding return.
 */
@Interceptor
@Policy
public class PolicyInterceptor {

    @AroundInvoke
    Object invoke(InvocationContext context) throws Exception {
        Policy policy = context.getInterceptorBinding(Policy.class);
        List<String> details = new ArrayList<>();
        for (Detail detail : policy.details()) {
            details.add(detail.value());
        }
        Calls.RECORDED.add(policy.detail().value() + " " + policy.detail().note().value() + " "
            + policy.fallback().value() + " " + details);
        return context.proceed();
    }
}
