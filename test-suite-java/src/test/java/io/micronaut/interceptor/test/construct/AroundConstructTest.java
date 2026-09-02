package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AroundConstructTest {

    @Test
    void interposesOnTheConstructor() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ConstructInterceptor.CALLS.clear();

            BuiltService service = context.createBean(BuiltService.class, "original");

            assertEquals("replaced", service.name());
            assertEquals(
                List.of("target before null",
                    "constructor BuiltService",
                    "parameters [original]",
                    "proceed returned null",
                    "target after replaced"),
                ConstructInterceptor.CALLS);
        }
    }

    /**
     * The bindings of a constructor are the ones its class is bound by together with the ones it declares itself.
     * The metadata of a constructor is the one place that does not already carry the annotations of its class, so
     * a binding declared on the class only reaches an {@code @AroundConstruct} interceptor because the processor
     * writes it onto the constructor.
     */
    @Test
    void showsTheInterceptorTheBindingsOfTheClassAsWell() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ConstructInterceptor.CALLS.clear();

            context.createBean(BuiltService.class, "original");

            assertEquals(Set.of("Built"), ConstructInterceptor.BINDINGS);
        }
    }

    /** 2.4 eb): there is no method for an {@code @AroundConstruct} interceptor method to be shown. */
    @Test
    void showsTheInterceptorNoMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ConstructInterceptor.CALLS.clear();

            context.createBean(BuiltService.class, "original");

            assertNull(ConstructInterceptor.method);
        }
    }
}
