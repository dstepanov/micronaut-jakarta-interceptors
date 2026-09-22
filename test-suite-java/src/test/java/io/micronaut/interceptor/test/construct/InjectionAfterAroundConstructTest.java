package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InjectionAfterAroundConstructTest {

    /**
     * 2.3 dd) completes the injection of an object only once every around-construct method has completed. The
     * constructor runs where the last around-construct method proceeds, and the fields and methods of the object are
     * injected, and its post-construct callback runs, after the whole chain has returned.
     */
    @Test
    void injectsAndInitializesTheObjectAfterTheAroundConstructMethodsReturn() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(Stages.class);
            Stages.RECORDED.clear();

            context.getBean(StagedService.class);

            assertEquals(List.of(
                "first before proceed",
                "second before proceed",
                "target constructed",
                "second after proceed",
                "first after proceed",
                "target injected",
                "target postConstruct"), Stages.RECORDED.subList(4, Stages.RECORDED.size()));
        }
    }
}
