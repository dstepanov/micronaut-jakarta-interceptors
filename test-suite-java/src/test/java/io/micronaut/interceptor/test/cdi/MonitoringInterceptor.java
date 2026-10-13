package io.micronaut.interceptor.test.cdi;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.MicronautInvocationContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Interceptor
@Monitored
public class MonitoringInterceptor {

    /**
     * The bindings the last invocation reported, by each accessor of them.
     */
    public static final List<String> REPORTED = new ArrayList<>();

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("monitored");
        report(context);
        return context.proceed();
    }

    private static void report(InvocationContext context) {
        REPORTED.clear();
        Set<String> values = new TreeSet<>();
        for (AnnotationValue<?> value : ((MicronautInvocationContext) context).getInterceptorBindingValues()) {
            String name = value.getAnnotationName();
            values.add(name.substring(name.lastIndexOf('.') + 1));
        }
        REPORTED.add("values " + values);
        Set<String> instances = new TreeSet<>();
        for (Annotation binding : context.getInterceptorBindings()) {
            instances.add(binding.annotationType().getSimpleName());
        }
        REPORTED.add("instances " + instances);
        REPORTED.add("secure " + (context.getInterceptorBinding(Secure.class) != null)
            + " " + context.getInterceptorBindings(Secure.class).size());
    }
}
