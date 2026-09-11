package io.micronaut.interceptor.test.hierarchy;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An interceptor class inherits a protected and a package private interceptor method from a superclass in another
 * package. The specification allows an interceptor method any access level and invokes the ones of a superclass
 * first, so both run - neither is a compilation error for being out of reach of the code generated beside the
 * subclass.
 */
class InterceptorMethodsOfASuperclassInAnotherPackageTest {

    @Test
    void invokesTheInterceptorMethodsASuperclassInAnotherPackageDeclares() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Hierarchy.CALLS.clear();
            RemoteService service = context.getBean(RemoteService.class);
            assertEquals(List.of("remote post"), List.copyOf(Hierarchy.CALLS));

            Hierarchy.CALLS.clear();
            assertEquals("worked", service.work());
            assertEquals(List.of("remote around", "own around", "work"), List.copyOf(Hierarchy.CALLS));
        }
    }
}
