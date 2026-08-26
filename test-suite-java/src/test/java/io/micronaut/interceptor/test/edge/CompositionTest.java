package io.micronaut.interceptor.test.edge;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositionTest {

    /**
     * A bean may be intercepted by this module and by an ordinary Micronaut advice at once. The two are one chain,
     * ordered as Micronaut orders any interceptors.
     */
    @Test
    void composesWithANativeMicronautAdvice() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Log.RECORDED.clear();

            assertEquals("done", context.getBean(MixedService.class).work());
            assertEquals(
                List.of("micronaut advice in", "Alpha on nothing", "target", "micronaut advice out"),
                Log.RECORDED);
        }
    }

    /**
     * The interception of the specification is synchronous. A method that returns a future is intercepted as it
     * hands the future back, not as the future completes, which is what an interceptor of a plain Micronaut
     * advice sees as well.
     */
    @Test
    void interposesOnTheCallRatherThanOnTheCompletionOfAFuture() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            Log.RECORDED.clear();

            CompletableFuture<String> future = context.getBean(AsyncService.class).later();

            assertEquals("later", future.get());
            assertEquals(List.of("Alpha on nothing", "body ran"), Log.RECORDED);
        }
    }
}
