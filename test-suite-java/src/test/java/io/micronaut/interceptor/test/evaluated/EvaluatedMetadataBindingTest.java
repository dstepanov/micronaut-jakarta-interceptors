package io.micronaut.interceptor.test.evaluated;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bindings an interceptor reads are the bindings the chain was resolved from, whatever else the method declares.
 *
 * <p>A binding a method declares replaces the one of the same type its class declares, the whole of it, so a method
 * declaring the binding again and leaving its member to the default is bound by the default. Micronaut wraps the
 * metadata of a method carrying an evaluated expression anywhere in its annotations, and that wrapper builds an
 * annotation out of the class's members and the method's merged together, which resurrects the value the method
 * replaced.</p>
 */
class EvaluatedMetadataBindingTest {

    @Test
    void readsTheBindingTheMethodDeclaresRatherThanTheOneItReplaced() {
        try (ApplicationContext context = ApplicationContext.run()) {
            EvaluatedZoneService service = context.getBean(EvaluatedZoneService.class);

            Calls.RECORDED.clear();
            assertEquals("by default", service.replacedByDefault());
            assertEquals(List.of(
                    "default zone interceptor",
                    "getInterceptorBinding default",
                    "getInterceptorBindings(Zone) default",
                    "getInterceptorBindings default"),
                Calls.RECORDED);
        }
    }

    @Test
    void readsTheBindingOfTheClassWhereTheMethodReplacesNothing() {
        try (ApplicationContext context = ApplicationContext.run()) {
            EvaluatedZoneService service = context.getBean(EvaluatedZoneService.class);

            Calls.RECORDED.clear();
            assertEquals("inherited", service.inherited());
            assertEquals(List.of("class zone interceptor"), Calls.RECORDED);
        }
    }
}
