package io.micronaut.interceptor.test.micronautapi;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An interceptor that reads the interception through the Micronaut view of the context sees everything the
 * specification's own accessors describe, and reflects on nothing to do it.
 */
class MicronautApiTest {

    @Test
    void anInterceptorReadsTheInterceptionWithoutReflecting() {
        Recorded.VALUES.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            CompiledService service = context.createBean(CompiledService.class, "Denis");
            assertEquals("hello Denis", service.greet("hello"));

            assertEquals(
                List.of("AROUND_CONSTRUCT CompiledService(1) same=true",
                    "POST_CONSTRUCT CompiledService.started region=users",
                    "AROUND CompiledService.greet region=users parameters=[greeting]"),
                List.copyOf(Recorded.VALUES));
        }
    }

    /**
     * A lifecycle interception runs one chain for the event, so the executable method it carries is not "the
     * callback this is the interception of" - there are two. It is the last one the event invokes, the one the
     * bean itself declares, which is what the specification has getMethod() answer with as well.
     */
    @Test
    void theEventOfAHierarchyIsDescribedByTheCallbackTheBeanDeclares() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Recorded.VALUES.clear();
            context.createBean(CompiledService.class, "Denis");

            assertTrue(
                Recorded.VALUES.stream().anyMatch(v -> v.startsWith("POST_CONSTRUCT CompiledService.started ")),
                () -> "expected the bean's own callback, got " + Recorded.VALUES);
            assertTrue(
                Recorded.VALUES.stream().noneMatch(v -> v.contains("CompiledBase.startedBase")),
                () -> "the superclass callback is not what the event is described by, got " + Recorded.VALUES);
        }
    }
}