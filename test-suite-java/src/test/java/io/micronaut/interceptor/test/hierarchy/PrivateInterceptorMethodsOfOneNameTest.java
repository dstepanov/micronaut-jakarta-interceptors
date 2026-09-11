package io.micronaut.interceptor.test.hierarchy;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An interceptor class and its superclass each declare a private interceptor method of one name and signature. Neither
 * overrides the other, so both run, the superclass one first - each of them once, rather than the first one found by
 * its name twice.
 */
class PrivateInterceptorMethodsOfOneNameTest {

    @Test
    void eachPrivateInterceptorMethodOfAHierarchyRunsOnce() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Hierarchy.CALLS.clear();
            LayeredService service = context.getBean(LayeredService.class);
            assertEquals(List.of("base post", "own post"), List.copyOf(Hierarchy.CALLS));

            Hierarchy.CALLS.clear();
            assertEquals("worked", service.work());
            assertEquals(List.of("base around", "own around", "work"), List.copyOf(Hierarchy.CALLS));
        }
    }
}
