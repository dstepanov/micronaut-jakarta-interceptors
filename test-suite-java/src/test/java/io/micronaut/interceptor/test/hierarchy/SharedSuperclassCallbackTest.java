package io.micronaut.interceptor.test.hierarchy;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two beans bound to different interceptors inherit their first {@code @PostConstruct} callback from the same
 * superclass. Each is intercepted by the interceptor it is bound to, whichever of them is created first: what a
 * lifecycle chain belongs to is the bean, not the class that happens to declare the callback it starts at.
 */
class SharedSuperclassCallbackTest {

    @Test
    void eachBeanOfAHierarchyIsInterceptedByItsOwnChain() {
        assertCalls(Tank.class, Glider.class,
            List.of("armed", "vehicle", "tank"), List.of("winged", "vehicle", "glider"));
    }

    @Test
    void andSoWhicheverOfThemIsCreatedFirst() {
        assertCalls(Glider.class, Tank.class,
            List.of("winged", "vehicle", "glider"), List.of("armed", "vehicle", "tank"));
    }

    private static void assertCalls(Class<?> first, Class<?> second, List<String> expectedFirst,
                                    List<String> expectedSecond) {
        try (ApplicationContext context = ApplicationContext.run()) {
            Hierarchy.CALLS.clear();
            context.getBean(first);
            assertEquals(expectedFirst, List.copyOf(Hierarchy.CALLS));

            Hierarchy.CALLS.clear();
            context.getBean(second);
            assertEquals(expectedSecond, List.copyOf(Hierarchy.CALLS));
        }
    }
}
