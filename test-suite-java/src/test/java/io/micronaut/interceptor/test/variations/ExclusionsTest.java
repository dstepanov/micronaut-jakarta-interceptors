package io.micronaut.interceptor.test.variations;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExclusionsTest {

    @Test
    void aConstructorMayExcludeTheInterceptorsOfItsClass() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();

            assertEquals("done", context.getBean(ExcludingConstructorService.class).work());
            // the construction is not intercepted, the business method still is
            assertEquals(List.of("constructor", "interceptor aroundInvoke", "work"), Calls.RECORDED);
        }
    }
}
