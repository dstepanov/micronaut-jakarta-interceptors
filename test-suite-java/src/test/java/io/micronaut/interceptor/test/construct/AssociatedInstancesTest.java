package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssociatedInstancesTest {

    /**
     * 2.3 da): an {@code @AroundConstruct} method runs only once injection has completed on the instances of all the
     * interceptor classes associated with the object, and not only on those that take part in its construction. The
     * interceptor that declares nothing but an {@code @AroundInvoke} method is created and injected before the
     * around-construct chain starts, which is also what the reference implementation does.
     */
    @Test
    void createsEveryAssociatedInterceptorBeforeTheConstructionStarts() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(Stages.class);
            Stages.RECORDED.clear();

            assertEquals("done", context.getBean(AssociatedService.class).work());

            assertEquals(List.of(
                "construct interceptor created",
                "invoke interceptor created",
                "invoke interceptor injected",
                "construct interceptor before proceed",
                "target constructed",
                "construct interceptor after proceed",
                "invoke interceptor around"), Stages.RECORDED);
        }
    }
}
