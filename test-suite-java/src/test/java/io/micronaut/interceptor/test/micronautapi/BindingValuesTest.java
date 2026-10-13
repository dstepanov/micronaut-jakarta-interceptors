package io.micronaut.interceptor.test.micronautapi;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bindings an interceptor was selected by, read as values through the Micronaut view of the context: the nearest
 * declaration of each, the members that do not bind included, with nothing built and nothing reflected on.
 *
 * <p>The annotation metadata of the element is ordinary Micronaut metadata, which merges the members a method leaves
 * out of a binding with the ones its class declares; the values do not, since the method's declaration is the
 * binding in effect, whole.</p>
 */
class BindingValuesTest {

    @Test
    void reportsTheBindingTheMethodDeclaresWhole() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BandedInterceptors.RECORDED.clear();
            assertEquals("replaced", context.getBean(BandedService.class).replaced());

            assertEquals(List.of(
                    "default value=(default) note=the method's",
                    "default metadata value=class"),
                BandedInterceptors.RECORDED);
        }
    }

    @Test
    void reportsTheBindingOfTheClassWhereTheMethodDeclaresNone() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BandedInterceptors.RECORDED.clear();
            assertEquals("inherited", context.getBean(BandedService.class).inherited());

            assertEquals(List.of(
                    "class value=class note=(default)",
                    "class metadata value=class"),
                BandedInterceptors.RECORDED);
        }
    }
}
