package io.micronaut.interceptor.test.edge;

import io.micronaut.context.ApplicationContext;
import io.micronaut.interceptor.runtime.InterceptorChainResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * The chains are remembered per intercepted element, not per object an invocation passes through: creating a
     * great many beans, each with its own lifecycle callbacks, must not grow what is held.
     */
    @Test
    void remembersOneChainPerElementRatherThanPerBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            for (int i = 0; i < 200; i++) {
                context.createBean(PrototypeService.class).work();
            }

            assertTrue(resolver.cachedChains() < 20,
                "one chain for each element, not for each bean: " + resolver.cachedChains());
        }
    }
}
