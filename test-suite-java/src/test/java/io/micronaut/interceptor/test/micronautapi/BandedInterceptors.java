package io.micronaut.interceptor.test.micronautapi;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.MicronautInvocationContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Interceptors that record what the Micronaut view of the context reports of the bindings: the effective bindings as
 * values, and what the annotation metadata of the element says of the same binding.
 */
final class BandedInterceptors {

    static final List<String> RECORDED = new ArrayList<>();

    private BandedInterceptors() {
    }

    static void record(String interceptor, InvocationContext context) {
        MicronautInvocationContext micronaut = (MicronautInvocationContext) context;
        for (AnnotationValue<?> binding : micronaut.getInterceptorBindingValues()) {
            if (binding.getAnnotationName().equals(Banded.class.getName())) {
                RECORDED.add(interceptor + " value=" + binding.stringValue("value").orElse("(default)")
                    + " note=" + binding.stringValue("note").orElse("(default)"));
            }
        }
        RECORDED.add(interceptor + " metadata value="
            + micronaut.getAnnotationMetadata().stringValue(Banded.class, "value").orElse("(none)"));
    }

    /** Bound to the default value, which is what the method replacing the binding of its class declares. */
    @Interceptor
    @Banded
    public static class DefaultBandInterceptor {

        @AroundInvoke
        public Object intercept(InvocationContext context) throws Exception {
            record("default", context);
            return context.proceed();
        }
    }

    /** Bound to the value of the class, which the method that declares nothing inherits. */
    @Interceptor
    @Banded("class")
    public static class ClassBandInterceptor {

        @AroundInvoke
        public Object intercept(InvocationContext context) throws Exception {
            record("class", context);
            return context.proceed();
        }
    }
}
