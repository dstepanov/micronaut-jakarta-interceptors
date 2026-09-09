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
     * <p>This source set also holds {@code @Scheduled} beans, which the scheduler creates on a thread of its own a
     * few milliseconds after the context starts, and whose chains are resolved whenever that lands. They are
     * created once, so the count settles; what this asserts is that it stays settled while two hundred more beans
     * of one element pass through. Measuring from the first bean instead is a race with that thread, which is what
     * this test used to lose intermittently.</p>
     */
    @Test
    void remembersOneChainPerElementRatherThanPerBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            context.createBean(PrototypeService.class).work();
            int settled = settledChainCount(resolver);

            for (int i = 0; i < 200; i++) {
                context.createBean(PrototypeService.class).work();
            }

            assertEquals(settled, resolver.cachedChains(),
                "the chains of one element are resolved once, however many beans pass through them");
        }
    }

    /**
     * The number of chains once the beans created by anything other than this test have been resolved, which is
     * the count holding still across consecutive reads.
     */
    private static int settledChainCount(InterceptorChainResolver resolver) {
        int previous = -1;
        int stable = 0;
        for (int attempt = 0; attempt < 100; attempt++) {
            int current = resolver.cachedChains();
            stable = current == previous ? stable + 1 : 0;
            if (stable == 5) {
                return current;
            }
            previous = current;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the chain count to settle", e);
            }
        }
        throw new AssertionError("The chain count never settled, last seen " + previous);
    }
}