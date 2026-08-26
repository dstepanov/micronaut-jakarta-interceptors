package io.micronaut.interceptor.test.construct;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Interceptor
@Built
public class ConstructInterceptor {

    public static final List<String> CALLS = new ArrayList<>();

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        CALLS.add("target before " + context.getTarget());
        CALLS.add("constructor " + context.getConstructor().getDeclaringClass().getSimpleName());
        CALLS.add("parameters " + Arrays.toString(context.getParameters()));
        // the arguments of the constructor may be replaced before it runs
        context.setParameters(new Object[]{"replaced"});
        // constructing produces no value of its own: proceed returns null, and the instance is the target
        CALLS.add("proceed returned " + context.proceed());
        CALLS.add("target after " + ((BuiltService) context.getTarget()).name());
    }
}
