package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AroundConstructInstancesTest {

    /**
     * 2.3 da): an {@code @AroundConstruct} method runs only once injection has completed on the instances of the
     * interceptor classes of the object, not only on its own. The second interceptor is created and injected before
     * the first one starts, not when the first one proceeds to it.
     */
    @Test
    void createsEveryInterceptorOfTheChainBeforeTheFirstOneRuns() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(Stages.class);
            Stages.RECORDED.clear();

            context.getBean(StagedService.class);

            assertEquals(List.of(
                "first interceptor created",
                "first interceptor injected",
                "second interceptor created",
                "second interceptor injected",
                "first before proceed",
                "second before proceed",
                "target constructed"), Stages.RECORDED.subList(0, 7));
        }
    }
}
