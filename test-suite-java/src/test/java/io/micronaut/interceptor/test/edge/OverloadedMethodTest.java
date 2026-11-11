package io.micronaut.interceptor.test.edge;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverloadedMethodTest {

    /**
     * Two methods of one name, taking the same number of arguments, are different elements: each is intercepted by
     * what it declares and not by what its namesake declares.
     */
    @Test
    void tellsOverloadsApart() {
        try (ApplicationContext context = ApplicationContext.run()) {
            OverloadedService service = context.getBean(OverloadedService.class);

            Log.RECORDED.clear();
            assertEquals("string a", service.work("a"));
            assertEquals(List.of("Alpha on String"), Log.RECORDED);

            Log.RECORDED.clear();
            assertEquals("integer 1", service.work(1));
            assertEquals(List.of("Beta on Integer"), Log.RECORDED);
        }
    }
}
