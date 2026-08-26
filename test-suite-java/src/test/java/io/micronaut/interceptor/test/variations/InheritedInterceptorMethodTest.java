package io.micronaut.interceptor.test.variations;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InheritedInterceptorMethodTest {

    @Test
    void anInterceptorMethodMayBeInheritedFromASuperclass() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();

            assertEquals("done", context.getBean(InheritingService.class).work());
            assertEquals(List.of("inherited interceptor method", "target"), Calls.RECORDED);
        }
    }
}
