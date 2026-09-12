package io.micronaut.interceptor.test.adapter;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bean Micronaut generates for an adapted method is not a bean the application declared, and the specification
 * has nothing to say about its life: it is the object the application declared that is constructed, initialized and
 * removed, and the interceptors bound to that object interpose on it once.
 *
 * <p>The generated bean carries the annotations of the class whose method is adapted, the binding among them, so
 * without being left out it is constructed, initialized and removed as an intercepted object of its own - giving the
 * interceptor a second instance, and handing it an object that is not of the type the binding was declared on.</p>
 */
class AdapterLifecycleTest {

    @Test
    void doesNotInterceptTheLifeOfTheGeneratedBean() {
        Calls.RECORDED.clear();
        TracingLifecycleInterceptor.created = 0;
        try (ApplicationContext context = ApplicationContext.run()) {
            AdaptedService service = context.getBean(AdaptedService.class);
            assertEquals(List.of("construct", "post construct the bean"), List.copyOf(Calls.RECORDED));

            Calls.RECORDED.clear();
            context.getBean(ApplicationEventPublisher.class).publishEvent(new Signal("fired"));
            assertEquals(List.of("invoke onSignal", "observed fired"), List.copyOf(Calls.RECORDED));

            Calls.RECORDED.clear();
            assertEquals("adapted", service.name());
            assertEquals(List.of("invoke name"), List.copyOf(Calls.RECORDED));
        }
        assertEquals(1, TracingLifecycleInterceptor.created,
            "one interceptor instance serves the one object the specification sees");
    }
}
