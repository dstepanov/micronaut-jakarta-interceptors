package io.micronaut.interceptor.test.factorylifecycle;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The lifecycle callback interceptors a class level binding of a factory binds run on the factory, not on the beans
 * its producer methods produce: a produced object is no instance of the factory (CDI 4.1 sections 7.2 and 9).
 */
class FactoryLifecycleInterceptionTest {

    @Test
    void theLifecycleInterceptorsOfAFactoryDoNotRunOnItsProduct() {
        Watcher.EVENTS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("part", context.getBean(Part.class).name());
        }
        assertEquals(List.of(), Watcher.EVENTS.stream().filter(event -> event.endsWith(" Part")).toList(),
            "events: " + Watcher.EVENTS);
        assertEquals(List.of("around construct Assembly", "post construct Assembly", "pre destroy Assembly"),
            Watcher.EVENTS.stream().filter(event -> event.endsWith(" Assembly")).toList(),
            "events: " + Watcher.EVENTS);
    }
}
