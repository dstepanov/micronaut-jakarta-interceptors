package io.micronaut.interceptor.test.lifecycle;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 2.4 j) of the specification: an interceptor method that does not call {@code proceed} leaves no
 * subsequent interceptor method and no lifecycle callback of the target class to run.
 *
 * <p>{@code ExceptionsTest} holds an {@code @AroundInvoke} chain to this. A lifecycle event is the other half of
 * the assertion, and the harder half: one chain runs for the event and every callback of the bean is invoked when
 * it is proceeded to the end, so a chain that stops has to keep all of them from running rather than the one it
 * was started for.</p>
 */
class HaltedLifecycleTest {

    @Test
    void anInterceptorThatDoesNotProceedStopsTheRestOfEachEvent() {
        Calls.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            HaltedService service = context.getBean(HaltedService.class);

            assertEquals(List.of("halting postConstruct"), List.copyOf(Calls.RECORDED),
                "neither the interceptor after it nor the callback of the bean");

            Calls.RECORDED.clear();
            context.destroyBean(service);

            assertEquals(List.of("halting preDestroy"), List.copyOf(Calls.RECORDED),
                "the same of a pre-destroy event");
        }
    }
}
