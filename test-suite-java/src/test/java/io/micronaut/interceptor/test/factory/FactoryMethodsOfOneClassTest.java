package io.micronaut.interceptor.test.factory;

import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two factory methods produce the same class, each bound to a different interceptor. The beans are instances of one
 * class with the same business method and the same callbacks, and each is still intercepted by what its own factory
 * method binds, for its lifecycle as for its business methods.
 */
class FactoryMethodsOfOneClassTest {

    @Test
    void eachProducedBeanIsInterceptedByWhatItsFactoryMethodBinds() {
        assertCalls("green", "yellow");
    }

    @Test
    void andSoWhicheverOfThemIsProducedFirst() {
        assertCalls("yellow", "green");
    }

    private static void assertCalls(String first, String second) {
        try (ApplicationContext context = ApplicationContext.run()) {
            for (String name : List.of(first, second)) {
                Calls.clear();
                Widget widget = context.getBean(Widget.class, Qualifiers.byName(name));
                assertEquals("worked", widget.work());
                assertEquals(List.of(name + " post", name + " invoke", "work"), List.copyOf(Calls.RECORDED), name);
            }
        }
    }
}
