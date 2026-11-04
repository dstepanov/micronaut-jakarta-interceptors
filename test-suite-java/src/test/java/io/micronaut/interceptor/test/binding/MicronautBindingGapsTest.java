package io.micronaut.interceptor.test.binding;

import io.micronaut.aop.ExactInterceptorBindingQualifier;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.interceptor.spi.InterceptorBindings;
import io.micronaut.interceptor.spi.InterceptorMethods;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What Micronaut's own selection of interceptors, {@link ExactInterceptorBindingQualifier} and the order it gives
 * them, still answers differently from the specification, and so what this module keeps doing itself. Each test
 * asserts Micronaut's answer as it is today beside the module's: once Micronaut answers as the specification does,
 * the test fails, and the module code it stands for can go.
 *
 * <p>Two bindings are compared with {@link AnnotationValue#matches(AnnotationValue)} itself, which compares an empty
 * string, an enum constant, a class and a primitive array as the specification does; {@code BindingValuesTest}
 * covers it.</p>
 */
class MicronautBindingGapsTest {

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    public @interface Gap {
        String value() default "";
    }

    @Interceptor
    @Gap
    @Priority(100)
    public static class GapInterceptor {
        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Interceptor
    @Gap
    public static class UnprioritisedGapInterceptor {
        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Singleton
    @Gap("class")
    public static class GapService {
        @Gap("")
        public String work() {
            return "worked";
        }
    }

    @Test
    void theBindingsOfAJakartaInterceptorAreNotMicronautBindings() {
        // Micronaut's exact qualifier reads the bindings of a point and of a candidate from the
        // io.micronaut.aop.InterceptorBinding values of their metadata, with a method's occurrences replacing its
        // class's. A Jakarta binding carries none, so the processor records the bindings, the method's replacing
        // the class's, and the module hands those to the rule
        try (ApplicationContext context = ApplicationContext.run()) {
            BeanDefinition<GapInterceptor> interceptor = context.getBeanDefinition(GapInterceptor.class);
            AnnotationMetadata work = context.getBeanDefinition(GapService.class).getRequiredMethod("work").getAnnotationMetadata();

            assertFalse(ExactInterceptorBindingQualifier.of(work, InterceptorKind.AROUND).matches(interceptor.getAnnotationMetadata()),
                "Micronaut");
            assertTrue(InterceptorBindings.of(interceptor).isBoundBy(InterceptorBindings.ofInterceptionPoint(work)), "module");
        }
    }

    @Test
    void anInterceptorClassWithoutAPriorityIsOrderedAtTheApplicationPriority() {
        // Micronaut maps @Priority onto its @Order, which the module reads as well, but orders a class declaring
        // neither at 0, ahead of every interceptor class of the application, where the module orders it at
        // Interceptor.Priority.APPLICATION
        try (ApplicationContext context = ApplicationContext.run()) {
            BeanDefinition<GapInterceptor> prioritised = context.getBeanDefinition(GapInterceptor.class);
            BeanDefinition<UnprioritisedGapInterceptor> unprioritised = context.getBeanDefinition(UnprioritisedGapInterceptor.class);

            assertEquals(100, OrderUtil.getOrder(prioritised.getAnnotationMetadata()), "Micronaut");
            assertEquals(100, InterceptorMethods.priorityOf(prioritised), "module");
            assertEquals(0, OrderUtil.getOrder(unprioritised.getAnnotationMetadata()), "Micronaut");
            assertEquals(Interceptor.Priority.APPLICATION, InterceptorMethods.priorityOf(unprioritised), "module");
        }
    }

    @Test
    void aTimeoutIsAKindOfInterceptionOfItsOwn() {
        // Micronaut has a kind for a method, a constructor and the two lifecycle events, but none for an
        // @AroundTimeout method, which the module intercepts a scheduled method with
        assertTrue(Arrays.stream(InterceptorKind.values()).noneMatch(kind -> kind.name().contains("TIMEOUT")), "Micronaut");
    }
}
