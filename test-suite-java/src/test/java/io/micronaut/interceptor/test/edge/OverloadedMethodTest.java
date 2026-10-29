package io.micronaut.interceptor.test.edge;

import io.micronaut.context.ApplicationContext;
import io.micronaut.interceptor.internal.runtime.InterceptorChainResolver;
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
     * <p>The {@code @Scheduled} beans of this source set, whose chains the scheduler resolves on a thread of its own,
     * are not in this context: see {@link io.micronaut.interceptor.test.ScheduledFixtures}. The count is read once
     * the first bean is created, and must not move while two hundred more beans of one element pass through.</p>
     */
    @Test
    void remembersOneChainPerElementRatherThanPerBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            context.createBean(PrototypeService.class).work();
            int resolved = resolver.cachedChains();

            for (int i = 0; i < 200; i++) {
                context.createBean(PrototypeService.class).work();
            }

            assertEquals(resolved, resolver.cachedChains(),
                "the chains of one element are resolved once, however many beans pass through them");
        }
    }
}
