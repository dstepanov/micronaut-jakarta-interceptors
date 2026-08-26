package io.micronaut.interceptor.test.variations;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EveryKindTest {

    @Test
    void oneInterceptorClassMayInterposeOnEveryKind() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(EveryKindService.class).work());

            assertEquals(
                List.of("aroundConstruct", "postConstruct", "target postConstruct",
                    "aroundInvoke work", "target work"),
                Calls.RECORDED);
            Calls.clear();
        }
        assertEquals(List.of("preDestroy"), Calls.RECORDED);
    }
}
