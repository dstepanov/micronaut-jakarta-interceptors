package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 2.3 bb) and cc): the interceptor instances of an object go when the object is removed or fails to be created,
 * whatever else went wrong on the way. Each of these is a way the cleanup used to be skipped.
 */
class InterceptorCleanupTest {

    /**
     * The instances are created one after another as the object is created, and one of them failing to be created
     * fails the object. The ones already created are the instances of an object that failed to be created, and go.
     */
    @Test
    void destroysTheInstancesAlreadyCreatedWhenAnotherCannotBeCreated() {
        Destructions.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertThrows(RuntimeException.class, () -> context.getBean(RolledService.class));

            assertEquals(List.of("rolled first created", "rolled first destroyed"), Destructions.RECORDED);
        }
    }

    /**
     * An ordinary Micronaut interceptor of the same pre-destroy event, ordered before the Jakarta advice, may return
     * without proceeding, and then no Jakarta pre-destroy interceptor method runs. The object is still destroyed, so
     * its interceptor instances still have to be.
     */
    @Test
    void destroysTheInstancesWhenTheJakartaPreDestroyNeverRuns() {
        Destructions.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            SuppressedService bean = context.getBean(SuppressedService.class);
            assertEquals("done", bean.work());
            assertEquals(List.of("suppressed interceptor created"), Destructions.RECORDED);
            Destructions.RECORDED.clear();

            context.destroyBean(bean);

            assertEquals(List.of(
                "micronaut pre-destroy advice suppressed the rest",
                "suppressed interceptor destroyed"), Destructions.RECORDED);
        }
    }
}
