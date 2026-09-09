package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Section 2.4 k) and l) of the specification: the invocation of {@code proceed} returns {@code null} for a method
 * of type void, and for a lifecycle callback.
 */
class LifecycleProceedTest {

    @Test
    void proceedReturnsNullInALifecycleCallback() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(RecordedService.class);
        }
        assertTrue(Calls.RECORDED.contains("postConstruct proceed -> null"),
            "2.4 l) proceed returns null for a lifecycle callback: " + Calls.RECORDED);
        assertTrue(Calls.RECORDED.contains("postConstruct getMethod -> null"),
            "2.4 eb) getMethod is null for a lifecycle callback of a class that declares none: " + Calls.RECORDED);
        assertTrue(Calls.RECORDED.contains("postConstruct getTimer -> null"),
            "2.4 db) getTimer is null for a lifecycle callback: " + Calls.RECORDED);
        assertTrue(Calls.RECORDED.contains("postConstruct getParameters -> IllegalStateException"),
            "2.4 f) getParameters fails for a lifecycle callback: " + Calls.RECORDED);
    }

    /**
     * Section 2.4 f) again, for the other accessor: a lifecycle callback has no arguments, so replacing them
     * fails as reading them does. Asserted for both events, and the event still runs to its end afterwards.
     */
    @Test
    void setParametersFailsInALifecycleCallback() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            RecordedService service = context.getBean(RecordedService.class);
            assertTrue(Calls.RECORDED.contains("postConstruct setParameters -> IllegalStateException"),
                "setParameters fails for a post-construct event: " + Calls.RECORDED);

            Calls.clear();
            context.destroyBean(service);
            assertTrue(Calls.RECORDED.contains("preDestroy setParameters -> IllegalStateException"),
                "setParameters fails for a pre-destroy event: " + Calls.RECORDED);
        }
    }

    @Test
    void proceedReturnsNullForAVoidMethod() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(RecordedService.class).voidMethod();
            assertEquals(1, Calls.RECORDED.stream().filter(it -> it.equals("void proceed -> null")).count(),
                "2.4 k) proceed returns null for a void method: " + Calls.RECORDED);
        }
    }
}
