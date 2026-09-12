package io.micronaut.interceptor.test.reflection;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@code getMethod()} answers for a bean a factory method produces as it does for a bean that declares itself one.
 * The method it returns belongs to the produced class, which is not the class the processor visits, and inside a
 * native image such a lookup answers only for a member the image was told to keep.
 */
class ProducedMemberLookupTest {

    @Test
    void answersWithTheBusinessMethodOfTheProducedClass() {
        ProducedMemberInterceptor.businessMethod = null;
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ProducedService.class).work());
        }

        Method method = ProducedMemberInterceptor.businessMethod;
        assertNotNull(method, "the intercepted method of a produced bean is resolved");
        assertEquals("work", method.getName());
        assertEquals(ProducedService.class, method.getDeclaringClass());
    }
}
