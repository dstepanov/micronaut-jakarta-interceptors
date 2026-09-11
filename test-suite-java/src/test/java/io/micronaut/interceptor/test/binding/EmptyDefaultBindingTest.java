package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A member that defaults to an empty string or an empty array is filled in before the comparison as any other
 * default is, so a declaration that spells the empty default out and one that leaves it out are the same binding.
 */
class EmptyDefaultBindingTest {

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
    void bindsTheDefaultsLeftOutOnBothSides() {
        assertEquals("defaults", context.getBean(ShelvedService.class).leftToItsDefaults());

        assertEquals(List.of("unnamed shelf"), Calls.RECORDED);
    }

    @Test
    void bindsAnEmptyStringSpeltOutToTheDefaultLeftOut() {
        assertEquals("empty value", context.getBean(ShelvedService.class).emptyValueSpeltOut());

        assertEquals(List.of("unnamed shelf"), Calls.RECORDED);
    }

    @Test
    void bindsEveryEmptyDefaultSpeltOutToTheDefaultsLeftOut() {
        assertEquals("every default", context.getBean(ShelvedService.class).everyDefaultSpeltOut());

        assertEquals(List.of("unnamed shelf"), Calls.RECORDED);
    }

    @Test
    void leavesAloneAMethodBoundByAnotherValue() {
        assertEquals("named", context.getBean(ShelvedService.class).onANamedShelf());

        assertEquals(List.of(), Calls.RECORDED);
    }
}
