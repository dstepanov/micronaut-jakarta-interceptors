package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HierarchyOrderTest {

    /**
     * Sections 5.2 d), h) and i): the interceptor methods of a hierarchy are all invoked, the most general
     * superclass first, and those of the target class come after every interceptor class.
     */
    @Test
    void invokesEveryInterceptorMethodOfAHierarchyMostGeneralFirst() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(HierarchicalService.class).work());

            List<String> around = Calls.RECORDED.stream()
                .map(it -> it.contains(" instance=") ? it.substring(0, it.indexOf(" instance=")) : it)
                .toList();
            assertEquals(
                List.of("grandparent postConstruct", "own postConstruct",
                    "grandparent around", "parent around", "own around",
                    "target superclass", "target own", "target"),
                around);
        }
    }

    /** 2.3 a): one interceptor instance serves every interceptor method of the hierarchy. */
    @Test
    void usesOneInterceptorInstanceForTheWholeHierarchy() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(HierarchicalService.class).work();

            long instances = Calls.RECORDED.stream()
                .filter(it -> it.contains(" instance="))
                .map(it -> it.substring(it.indexOf(" instance=")))
                .distinct()
                .count();
            assertEquals(1, instances, Calls.RECORDED.toString());
        }
    }
}
