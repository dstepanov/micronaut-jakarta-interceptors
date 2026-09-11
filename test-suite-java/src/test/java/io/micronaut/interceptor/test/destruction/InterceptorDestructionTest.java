package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 2.3 a) and bb): an interceptor instance has the life of the object it intercepts, so it is destroyed when that
 * object is, and when that object fails to be created. What is destroyed with it is what it holds of its own.
 */
class InterceptorDestructionTest {

    @Test
    void destroysTheInterceptorsOfASingletonWhenTheContextCloses() {
        Destructions.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(OwnedSingleton.class).work());
        }
        assertEquals(List.of(
            "interceptor created",
            "around",
            // 2.3 cc): the object goes first, then the interceptor instances associated with it
            "target destroyed",
            "interceptor destroyed",
            "interceptor resource destroyed"), Destructions.RECORDED);
    }

    @Test
    void destroysTheInterceptorsOfAPrototypeWhenItIsDestroyed() {
        Destructions.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            OwnedPrototype first = context.getBean(OwnedPrototype.class);
            OwnedPrototype second = context.getBean(OwnedPrototype.class);
            assertEquals("done", first.work());
            assertEquals("done", second.work());
            Destructions.RECORDED.clear();

            context.destroyBean(first);

            // the interceptor of the other object is still in use, and stays
            assertEquals(List.of("interceptor destroyed", "interceptor resource destroyed"), Destructions.RECORDED);
            Destructions.RECORDED.clear();
            assertEquals("done", second.work());
            assertEquals(List.of("around"), Destructions.RECORDED);
        }
    }

    /**
     * A bean a factory produced loses its interceptor registrations as it is created: Micronaut takes the first of
     * the dependents it recorded for the bean to be the factory, and destroys it as soon as the bean exists. The
     * interceptor of the bean is not destroyed with it, and it is the same instance that interposes on the
     * destruction of the bean before it is destroyed itself.
     */
    @Test
    void keepsTheInterceptorsOfAProducedBeanUntilTheBeanIsDestroyed() {
        Destructions.RECORDED.clear();
        ProducedLifecycleInterceptor.reset();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("used", context.getBean(ProducedResource.class).use());

            assertEquals(List.of("post-construct on interceptor 1"), Destructions.RECORDED);
        }
        assertEquals(List.of(
            "post-construct on interceptor 1",
            "pre-destroy on interceptor 1",
            "interceptor 1 destroyed"), Destructions.RECORDED);
    }

    /**
     * An interceptor declared a singleton belongs to no one object. It goes with the context, like any other
     * singleton, and not with the first object it intercepted.
     */
    @Test
    void leavesASingletonInterceptorToTheContext() {
        Destructions.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            SharingPrototype first = context.getBean(SharingPrototype.class);
            SharingPrototype second = context.getBean(SharingPrototype.class);
            assertEquals("done", first.work());

            context.destroyBean(first);

            assertEquals(List.of(), Destructions.RECORDED);
            assertEquals("done", second.work());
        }
        assertEquals(List.of("shared interceptor destroyed"), Destructions.RECORDED);
    }

    @Test
    void destroysTheInterceptorsOfAnObjectWhoseConstructorFails() {
        Destructions.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertThrows(RuntimeException.class, () -> context.getBean(DoomedService.class));

            assertEquals(List.of("construct interceptor created", "construct interceptor destroyed"),
                Destructions.RECORDED);
        }
    }

    @Test
    void destroysTheInterceptorsOfAnObjectWhosePostConstructFails() {
        Destructions.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertThrows(RuntimeException.class, () -> context.getBean(UnreadyService.class));

            assertEquals(List.of("post-construct interceptor created", "post-construct interceptor destroyed"),
                Destructions.RECORDED);
        }
    }
}
