package io.micronaut.interceptor.test.handoff;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 2.3 a): a bean proxied with a separate target is one object to the specification and two to Micronaut, and it gets
 * one instance of each interceptor class. The proxy finds the instances of its target among the objects the thread
 * creating it has post-constructed, which the creation of the proxy itself adds to: it resolves its own advice, and
 * anything a {@code BeanCreatedEventListener} of the target asks for, after the target and before the proxy.
 */
class ProxyTargetHandoffTest {

    @Test
    void sharesTheInstancesOfATargetAcrossBeansCreatedInBetween() {
        HandedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(HandedTargetService.class).work());

            // one instance for the one object, not a second set for the proxy; the Meddled bean the listener
            // created in between has one of its own
            assertEquals(2, HandedInterceptor.CREATED.size());
            assertEquals(1, HandedInterceptor.INVOKED.size());
            assertSame(HandedInterceptor.CREATED.get(0), HandedInterceptor.INVOKED.get(0));
        }
    }

    @Test
    void sharesTheInstancesOfTheTargetOfABeanAFactoryProduced() {
        HandedInterceptor.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(HandedProduct.class).work());

            assertEquals(1, HandedInterceptor.CREATED.size());
            assertEquals(1, HandedInterceptor.INVOKED.size());
            assertSame(HandedInterceptor.CREATED.get(0), HandedInterceptor.INVOKED.get(0));
        }
        assertEquals(1, HandedInterceptor.DESTROYED.size());
        assertSame(HandedInterceptor.CREATED.get(0), HandedInterceptor.DESTROYED.get(0));
    }
}
