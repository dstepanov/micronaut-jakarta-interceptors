package io.micronaut.interceptor.test.selfintercept;

import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A class may declare an interceptor method on itself, which the specification invokes after every interceptor
 * class that applies to it.
 */
@Singleton
@Interceptors(TracingInterceptor.class)
public class SelfInterceptingService {

    public static final List<String> CALLS = new ArrayList<>();

    public String work() {
        CALLS.add("target");
        return "done";
    }

    @AroundInvoke
    Object aroundItsOwnMethods(InvocationContext context) throws Exception {
        CALLS.add("self in");
        try {
            return context.proceed();
        } finally {
            CALLS.add("self out");
        }
    }
}
