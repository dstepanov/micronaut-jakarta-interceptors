package io.micronaut.interceptor.test.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * What the interceptors saw of one invocation.
 */
public final class Observation {

    public static Object target;
    public static Method method;
    public static Constructor<?> constructor;
    public static Object timer;
    public static List<Object> parameters;
    public static Set<Annotation> bindings;
    public static List<String> contextData;

    private Observation() {
    }

    static void record(jakarta.interceptor.InvocationContext context) {
        target = context.getTarget();
        method = context.getMethod();
        constructor = context.getConstructor();
        timer = context.getTimer();
        parameters = Arrays.asList(context.getParameters());
        bindings = context.getInterceptorBindings();
    }
}
