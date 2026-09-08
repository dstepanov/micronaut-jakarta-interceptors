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
     *
     * <p>Measured as a difference rather than against a bound, since what an application holds in total is the
     * number of elements it intercepts, which the beans of this source set are free to change.</p>
     */
    @Test
    void remembersOneChainPerElementRatherThanPerBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            context.createBean(PrototypeService.class).work();
            int afterOne = resolver.cachedChains();

            for (int i = 0; i < 200; i++) {
                context.createBean(PrototypeService.class).work();
            }

            assertEquals(afterOne, resolver.cachedChains(),
                "the chains of one element are resolved once, however many beans pass through them");
        }
    }
}
