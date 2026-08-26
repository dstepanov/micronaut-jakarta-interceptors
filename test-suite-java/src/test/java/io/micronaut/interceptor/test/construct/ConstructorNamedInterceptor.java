package io.micronaut.interceptor.test.construct;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.List;

public class ConstructorNamedInterceptor {

    public static final List<String> CALLS = new ArrayList<>();

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        CALLS.add("named");
        context.proceed();
    }
}
