package io.micronaut.interceptor.test.micronautapi;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            assertEquals("hello Denis", service.greet());

            assertEquals(
                List.of("AROUND_CONSTRUCT CompiledService(1)",
                    "POST_CONSTRUCT CompiledService.started region=users",
                    "AROUND CompiledService.greet region=users"),
                List.copyOf(Recorded.VALUES));
        }
    }
}
