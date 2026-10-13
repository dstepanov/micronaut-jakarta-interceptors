package io.micronaut.interceptor.test.micronautapi;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.MicronautInvocationContext;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bindings an interceptor was selected by, read as values through the Micronaut view of the context: the nearest
 * declaration of each, the members that do not bind included, with nothing built and nothing reflected on.
 *
 * <p>The annotation metadata of the element is ordinary Micronaut metadata, which merges the members a method leaves
 * out of a binding with the ones its class declares; the values do not, since the method's declaration is the
 * binding in effect, whole.</p>
 */
class BindingValuesTest {

    @Test
    void reportsTheBindingTheMethodDeclaresWhole() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BandedInterceptors.RECORDED.clear();
            assertEquals("replaced", context.getBean(BandedService.class).replaced());

            assertEquals(List.of(
                    "default value=(default) note=the method's",
                    "default metadata value=class"),
                BandedInterceptors.RECORDED);
        }
    }

    @Test
    void reportsTheBindingOfTheClassWhereTheMethodDeclaresNone() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BandedInterceptors.RECORDED.clear();
            assertEquals("inherited", context.getBean(BandedService.class).inherited());

            assertEquals(List.of(
                    "class value=class note=(default)",
                    "class metadata value=class"),
                BandedInterceptors.RECORDED);
        }
    }

    /**
     * The metadata of a method holds the annotations of the method it overrides, and a binding declared there is not
     * one of the override's: the override is bound by its class, and that is the binding reported.
     */
    @Test
    void reportsTheBindingOfTheClassRatherThanTheOneOfTheOverriddenMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BandedInterceptors.RECORDED.clear();
            assertEquals("overridden", context.getBean(BandedService.class).overridden());

            assertEquals(List.of("class value=class note=(default)"), BandedInterceptors.RECORDED.subList(0, 1));
        }
    }

    /**
     * Two occurrences of a repeatable binding that differ only in a member that does not bind are one binding, which
     * the value and the instance accessors both report once.
     */
    @Test
    void reportsOccurrencesThatAreOneBindingOnce() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RemarkInterceptor.RECORDED.clear();
            assertEquals("remarked", context.getBean(RemarkedService.class).remarked());

            assertEquals(List.of("values [first]", "instances [first]", "all [first]"), RemarkInterceptor.RECORDED);
        }
    }

    @InterceptorBinding
    @Repeatable(Remarks.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Remark {
        String value();

        @Nonbinding
        String note() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Remarks {
        Remark[] value();
    }

    @Interceptor
    @Remark("same")
    public static class RemarkInterceptor {

        static final List<String> RECORDED = new ArrayList<>();

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            List<String> values = new ArrayList<>();
            for (AnnotationValue<?> binding : ((MicronautInvocationContext) context).getInterceptorBindingValues()) {
                values.add(binding.stringValue("note").orElse("(default)"));
            }
            RECORDED.add("values " + values);
            List<String> instances = new ArrayList<>();
            for (Remark remark : context.getInterceptorBindings(Remark.class)) {
                instances.add(remark.note());
            }
            RECORDED.add("instances " + instances);
            List<String> all = new ArrayList<>();
            for (Annotation binding : context.getInterceptorBindings()) {
                if (binding instanceof Remark remark) {
                    all.add(remark.note());
                }
            }
            RECORDED.add("all " + all);
            return context.proceed();
        }
    }

    @Singleton
    public static class RemarkedService {

        @Remark(value = "same", note = "first")
        @Remark(value = "same", note = "second")
        public String remarked() {
            return "remarked";
        }
    }
}
