package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A binding a method declares replaces the whole binding of the same type its class declares. The members the
 * method gives no value take their defaults, never the values of the class's declaration.
 */
class MethodBindingReplacementTest {

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
    void aMethodWithoutABindingOfItsOwnIsBoundByTheClass() {
        assertEquals("inherited", context.getBean(ZonedService.class).inherited());

        assertEquals(List.of("zone a"), Calls.RECORDED);
    }

    @Test
    void aMethodBindingLeftToItsDefaultReplacesTheClassBinding() {
        assertEquals("by default", context.getBean(ZonedService.class).replacedByDefault());

        assertEquals(List.of("zone z"), Calls.RECORDED);
    }

    @Test
    void aMethodBindingSpeltOutReplacesTheClassBinding() {
        assertEquals("spelt out", context.getBean(ZonedService.class).replacedSpeltOut());

        assertEquals(List.of("zone z"), Calls.RECORDED);
    }

    @Test
    void aMethodBindingWithOnlyAnExcludedMemberReplacesTheClassBinding() {
        assertEquals("comment", context.getBean(RegionedService.class).replacedByAComment());

        assertEquals(List.of("default region"), Calls.RECORDED);
    }
}
