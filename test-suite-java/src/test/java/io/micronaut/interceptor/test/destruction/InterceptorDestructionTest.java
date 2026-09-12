
package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Section 2.3: the interceptor instances associated with an object are destroyed when the object is, after its
 * pre-destroy interception has run - and with them whatever was injected into them.
 */
class InterceptorDestructionTest {

    @BeforeEach
    void clear() {
        Destruction.CALLS.clear();
    }

    @Test
    void destroysTheInterceptorsOfAnObjectDestroyedThroughItsRegistration() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BeanRegistration<ReleasedService> registration = context.getBeanRegistration(ReleasedService.class, null);
            registration.bean().work();
            registration.close();
            assertEquals(List.of("target destroyed", "resource of the interceptor destroyed"), Destruction.CALLS);
        }
    }

    @Test
    void destroysTheInterceptorsOfAnObjectDestroyedByItself() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ReleasedService service = context.getBean(ReleasedService.class);
            service.work();
            context.destroyBean(service);
            assertEquals(List.of("target destroyed", "resource of the interceptor destroyed"), Destruction.CALLS);
        }
    }

    @Test
    void leavesASingletonInterceptorToTheContext() {
        try (ApplicationContext context = ApplicationContext.run()) {
            SharedService first = context.getBean(SharedService.class);
            SharedService second = context.getBean(SharedService.class);
            first.work();
            second.work();
            context.destroyBean(first);
            assertFalse(Destruction.CALLS.contains("resource of the singleton interceptor destroyed"));
            // the interceptor still serves the objects that are left
            second.work();
        }
        assertTrue(Destruction.CALLS.contains("resource of the singleton interceptor destroyed"));
    }
}
