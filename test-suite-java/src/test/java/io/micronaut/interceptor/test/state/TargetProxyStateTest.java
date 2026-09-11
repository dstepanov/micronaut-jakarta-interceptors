package io.micronaut.interceptor.test.state;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 2.3 a): one interceptor instance serves every interception of one object, which is what lets it keep
 * state from the creation of that object to its invocations.
 *
 * <p>A bean proxied with a separate target is two objects to Micronaut - the proxy its business methods are invoked
 * on, and the target whose lifecycle is intercepted - each with advice of its own. To the specification it is one
 * target instance, and it gets one instance of each interceptor class.</p>
 */
class TargetProxyStateTest {

    @Test
    void sharesTheInterceptorOfAProxyTargetSingleton() {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ProxiedTargetService.class).work());

            assertOneInstance();
        }
        assertEquals(1, PairedInterceptor.DESTROYED.size());
        assertSame(PairedInterceptor.POST_CONSTRUCTED.get(0), PairedInterceptor.DESTROYED.get(0));
    }

    @Test
    void sharesTheInterceptorOfAProxyTargetPrototype() {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            ProxiedTargetPrototype first = context.getBean(ProxiedTargetPrototype.class);
            assertEquals("done", first.work());

            assertOneInstance();

            context.destroyBean(first);
            assertEquals(1, PairedInterceptor.DESTROYED.size());
            assertSame(PairedInterceptor.POST_CONSTRUCTED.get(0), PairedInterceptor.DESTROYED.get(0));
        }
    }

    /**
     * Destroyed through its registration rather than as an instance, Micronaut destroys the proxy first and the
     * target after it, with the advice of each.
     */
    @Test
    void sharesTheInterceptorOfAProxyTargetPrototypeDestroyedThroughItsRegistration() {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            BeanRegistration<ProxiedTargetPrototype> first = context.getBeanRegistration(ProxiedTargetPrototype.class, null);
            ProxiedTargetPrototype second = context.getBean(ProxiedTargetPrototype.class);
            assertEquals("done", first.getBean().work());
            assertEquals("done", second.work());

            // one instance for each object, and each object has its own
            assertEquals(2, PairedInterceptor.POST_CONSTRUCTED.size());
            assertEquals(PairedInterceptor.POST_CONSTRUCTED, PairedInterceptor.INVOKED);
            assertNotSame(PairedInterceptor.INVOKED.get(0), PairedInterceptor.INVOKED.get(1));

            context.destroyBean(first);
            assertEquals(List.of(PairedInterceptor.POST_CONSTRUCTED.get(0)), PairedInterceptor.DESTROYED);
        }
    }

    @Test
    void sharesTheInterceptorOfABeanAFactoryProduced() {
        PairedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ProducedService.class).work());

            assertOneInstance();
        }
        assertEquals(1, PairedInterceptor.DESTROYED.size());
        assertSame(PairedInterceptor.POST_CONSTRUCTED.get(0), PairedInterceptor.DESTROYED.get(0));
    }

    private static void assertOneInstance() {
        assertEquals(1, PairedInterceptor.POST_CONSTRUCTED.size());
        assertEquals(1, PairedInterceptor.INVOKED.size());
        PairedInterceptor invoked = PairedInterceptor.INVOKED.get(0);
        assertSame(PairedInterceptor.POST_CONSTRUCTED.get(0), invoked);
        // what the interceptor kept as the object was created is still there when the object is invoked
        assertNotNull(invoked.created());
    }
}
