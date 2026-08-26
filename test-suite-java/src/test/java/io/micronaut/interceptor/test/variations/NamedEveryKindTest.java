package io.micronaut.interceptor.test.variations;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamedEveryKindTest {

    @Test
    void anInterceptorNamedByTheClassInterposesOnEveryKind() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(NamedEveryKindService.class).work());

            assertEquals(
                List.of("named aroundConstruct", "named postConstruct", "named aroundInvoke work", "target work"),
                Calls.RECORDED);
            Calls.clear();
        }
        assertEquals(List.of("named preDestroy"), Calls.RECORDED);
    }
}
