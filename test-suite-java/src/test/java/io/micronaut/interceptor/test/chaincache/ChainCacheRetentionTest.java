package io.micronaut.interceptor.test.chaincache;

import io.micronaut.context.ApplicationContext;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * What a chain is remembered by must belong to no invocation.
 *
 * <p>Micronaut hands the advice the metadata of a method containing an evaluated expression wrapped around the
 * evaluation context of the invocation, which holds the object being invoked and the arguments it was invoked with.
 * Every annotation read from that metadata comes back wrapped the same way, so remembering a chain under the
 * annotation itself would keep the first invocation of such a method alive for as long as the resolver, which is for
 * as long as the application. The resolver remembers a chain under what the processor wrote instead - class objects
 * and strings - and what an invocation brought with it is no part of that.</p>
 */
class ChainCacheRetentionTest {

    @Test
    void doesNotHoldOnToTheArgumentsOfTheInvocationThatResolvedTheChain() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ExpressionService service = context.getBean(ExpressionService.class);
            Calls.RECORDED.clear();

            Object token = new Object();
            WeakReference<Object> held = new WeakReference<>(token);
            assertEquals("described", service.describe(token));
            assertEquals(List.of("bound interceptor"), Calls.RECORDED);

            token = null;
            assertNull(collect(held), "the argument of the first invocation of the method is still reachable");
        }
    }

    /**
     * Whatever the reference still holds once the collector has had several chances to clear it.
     */
    private static @Nullable Object collect(WeakReference<?> reference) {
        for (int attempt = 0; attempt < 20 && reference.get() != null; attempt++) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the collector", e);
            }
        }
        return reference.get();
    }
}
