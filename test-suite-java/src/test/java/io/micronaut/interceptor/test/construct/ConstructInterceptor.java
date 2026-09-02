package io.micronaut.interceptor.test.construct;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Interceptor
@Built
public class ConstructInterceptor {

    public static final List<String> CALLS = new ArrayList<>();

    /** The bindings the interceptor was shown, which for a constructor are the ones of its class as well. */
    public static final Set<String> BINDINGS = new LinkedHashSet<>();

    /** What {@code getMethod()} returned, which the specification has be {@code null} here. */
    public static Method method;

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        BINDINGS.clear();
        method = context.getMethod();
        for (Annotation binding : context.getInterceptorBindings()) {
            BINDINGS.add(binding.annotationType().getSimpleName());
        }
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
