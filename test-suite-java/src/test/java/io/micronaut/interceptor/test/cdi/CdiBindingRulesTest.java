package io.micronaut.interceptor.test.cdi;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The rules Jakarta Contexts and Dependency Injection adds to the binding of interceptors.
 */
class CdiBindingRulesTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @BeforeEach
    void clear() {
        Calls.clear();
    }

    @Test
    void aBindingDeclaredOnABindingBindsTransitively() {
        context.getBean(TransitiveService.class).critical();

        assertEquals(List.of("monitored", "critical"), Calls.RECORDED);
    }

    @Test
    void anInterceptorWithTwoBindingsNeedsBothOfThem() {
        context.getBean(TransitiveService.class).both();

        // all three declare no priority of their own, so they share one and are ordered by their class name
        assertEquals(List.of("both", "monitored", "secure", "both method"), Calls.RECORDED);
    }

    @Test
    void anInterceptorWithTwoBindingsIsNotBoundByOneOfThem() {
        context.getBean(TransitiveService.class).secureOnly();

        assertEquals(List.of("secure", "secureOnly"), Calls.RECORDED);
    }

    @Test
    void anInheritedBindingIsInheritedBySubclasses() {
        context.getBean(InheritingService.class).inheritedBinding();

        assertEquals(List.of("secure", "inheritedBinding"), Calls.RECORDED);
    }
}
