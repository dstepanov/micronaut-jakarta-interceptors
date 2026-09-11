package io.micronaut.interceptor.test.hierarchy;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two beans bound to different interceptors inherit one business method from the same superclass. The method is the
 * same method of the same class for both, and yet each bean is intercepted by the interceptor it is bound to,
 * whichever of them is invoked first: a chain belongs to what the interception of an element declares, not to the
 * class that happens to declare the method.
 */
class SharedSuperclassBusinessMethodTest {

    @Test
    void eachBeanIsInterceptedByItsOwnChain() {
        assertCalls(RedWorkshop.class, BlueWorkshop.class, List.of("red", "work"), List.of("blue", "work"));
    }

    @Test
    void andSoWhicheverOfThemIsInvokedFirst() {
        assertCalls(BlueWorkshop.class, RedWorkshop.class, List.of("blue", "work"), List.of("red", "work"));
    }

    private static void assertCalls(Class<? extends Workshop> first, Class<? extends Workshop> second,
                                    List<String> expectedFirst, List<String> expectedSecond) {
        try (ApplicationContext context = ApplicationContext.run()) {
            Hierarchy.CALLS.clear();
            assertEquals("worked", context.getBean(first).work());
            assertEquals(expectedFirst, List.copyOf(Hierarchy.CALLS));

            Hierarchy.CALLS.clear();
            assertEquals("worked", context.getBean(second).work());
            assertEquals(expectedSecond, List.copyOf(Hierarchy.CALLS));
        }
    }
}
