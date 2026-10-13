package io.micronaut.interceptor.test.drift;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.interceptor.spi.InterceptorBindings;
import io.micronaut.reflection.ReflectionAnnotations;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Micronaut keeps the defaults of an annotation in a registry shared by the whole application, and the reflection
 * module registers more of them - an empty string among them - the first time it builds an instance of the
 * annotation. The bindings read from what the processor recorded must not change with it: an element read before
 * and after would otherwise be two different interceptions, and an interceptor bound to it before would not be
 * after.
 */
class DefaultsDriftTest {

    @Test
    void readsTheRecordedBindingsAlikeBeforeAndAfterAnInstanceIsBuilt() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BeanDefinition<DriftingService> service = context.getBeanDefinition(DriftingService.class);
            BeanDefinition<DriftingInterceptor> interceptor = context.getBeanDefinition(DriftingInterceptor.class);
            AnnotationValue<Drifting> declared = service.getAnnotationMetadata().getAnnotation(Drifting.class);
            assertNotNull(declared);

            InterceptorBindings pointBefore = InterceptorBindings.ofInterceptionPoint(service.getAnnotationMetadata());
            InterceptorBindings interceptorBefore = InterceptorBindings.of(interceptor);
            InterceptorBindings declaredBefore = InterceptorBindings.ofValues(List.of(declared));

            Drifting instance = ReflectionAnnotations.synthesize(Drifting.class, declared);
            assertEquals("", instance.value());

            InterceptorBindings pointAfter = InterceptorBindings.ofInterceptionPoint(service.getAnnotationMetadata());
            InterceptorBindings interceptorAfter = InterceptorBindings.of(interceptor);
            InterceptorBindings declaredAfter = InterceptorBindings.ofValues(List.of(declared));

            assertEquals(pointBefore, pointAfter);
            assertEquals(pointBefore.values(), pointAfter.values());
            assertEquals(interceptorBefore, interceptorAfter);
            assertEquals(interceptorBefore.values(), interceptorAfter.values());
            assertEquals(declaredBefore, declaredAfter);
            assertTrue(interceptorAfter.isBoundBy(pointBefore));
            assertTrue(interceptorBefore.isBoundBy(pointAfter));
            assertTrue(interceptorAfter.isBoundBy(declaredBefore));
            assertTrue(interceptorBefore.isBoundBy(declaredAfter));
            assertTrue(interceptorAfter.isBoundBy(InterceptorBindings.ofValues(
                List.of(AnnotationValue.builder(Drifting.class).member("value", "").member("level", "INFO").build()))));

            assertEquals("drifting work", context.getBean(DriftingService.class).work());
        }
    }

    /**
     * What the bindings were read from, reduced to the members that bind, is there to be read back: Micronaut CDI
     * answers the bindings of an interceptor from it.
     */
    @Test
    void readsBackTheValuesTheBindingsWereReadFrom() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorBindings bindings = InterceptorBindings.of(context.getBeanDefinition(DriftingInterceptor.class));

            List<AnnotationValue<?>> values = bindings.values();
            assertEquals(1, values.size());
            AnnotationValue<?> value = values.get(0);
            assertEquals(Drifting.class.getName(), value.getAnnotationName());
            assertEquals("INFO", value.stringValue("level").orElse(null));
            assertEquals(bindings, InterceptorBindings.ofValues(values));
        }
    }
}
