package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
