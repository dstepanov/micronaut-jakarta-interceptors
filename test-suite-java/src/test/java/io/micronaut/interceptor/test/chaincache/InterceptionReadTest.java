package io.micronaut.interceptor.test.chaincache;

import io.micronaut.context.ApplicationContext;
import io.micronaut.interceptor.internal.runtime.InterceptorChainResolver;
import io.micronaut.interceptor.test.edge.PrototypeService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reading the interception of an element into what its chain is built from brings every binding of it into the
 * shape bindings are compared in, which is the costly part of resolving a chain. It is done once for each
 * interception an application records, not again for every bean created and every invocation that looks the chain up.
 */
class InterceptionReadTest {

    @Test
    void readsTheInterceptionOfAnElementOnceHoweverManyBeansPassThroughIt() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            context.createBean(PrototypeService.class).work();
            long settled = settled(resolver);

            for (int i = 0; i < 200; i++) {
                context.createBean(PrototypeService.class).work();
            }

            assertEquals(settled, resolver.readInterceptions(),
                "the interception of an element is read once, however many beans of it are created");
        }
    }

    /**
     * The count once the beans the scheduler creates on a thread of its own have been resolved, which is the count
     * holding still across consecutive reads.
     */
    private static long settled(InterceptorChainResolver resolver) {
        long previous = -1;
        int stable = 0;
        for (int attempt = 0; attempt < 100; attempt++) {
            long current = resolver.readInterceptions();
            stable = current == previous ? stable + 1 : 0;
            if (stable == 5) {
                return current;
            }
            previous = current;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the count to settle", e);
            }
        }
        throw new AssertionError("The count never settled, last seen " + previous);
    }
}
