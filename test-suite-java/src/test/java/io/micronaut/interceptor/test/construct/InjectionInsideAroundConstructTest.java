package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InjectionInsideAroundConstructTest {

    /**
     * 2.3 dd) completes the injection of an object only once every around-construct method has completed. This
     * module differs: Micronaut injects and initializes an object in the same generated method that constructs it,
     * and that method is what the last around-construct method proceeds to. So the object is injected, and its
     * post-construct callback has run, by the time {@code proceed()} returns to each around-construct method.
     *
     * <p>This pins the behaviour the guide documents as a difference, so that a Micronaut that separates the two
     * shows up here.</p>
     */
    @Test
    void injectsAndInitializesTheObjectBeforeTheAroundConstructMethodsReturn() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(Stages.class);
            Stages.RECORDED.clear();

            context.getBean(StagedService.class);

            assertEquals(List.of(
                "first before proceed",
                "second before proceed",
                "target constructed",
                "target injected",
                "target postConstruct",
                "second after proceed",
                "first after proceed"), Stages.RECORDED.subList(4, Stages.RECORDED.size()));
        }
    }
}
