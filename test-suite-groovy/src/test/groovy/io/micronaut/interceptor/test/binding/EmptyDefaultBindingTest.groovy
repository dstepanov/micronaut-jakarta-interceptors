package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * A member that defaults to an empty string is filled in before the comparison as any other default is, as it
 * is in Java.
 */
class EmptyDefaultBindingTest {

    private static ApplicationContext context

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run()
    }

    @AfterAll
    static void stopContext() {
        context.close()
    }

    @BeforeEach
    void clear() {
        ShelfCalls.RECORDED.clear()
    }

    @Test
    void bindsTheDefaultsLeftOutOnBothSides() {
        assertEquals("defaults", context.getBean(ShelvedService).leftToItsDefaults())
        assertEquals(["unnamed shelf"], ShelfCalls.RECORDED)
    }

    @Test
    void bindsAnEmptyStringSpeltOutToTheDefaultLeftOut() {
        assertEquals("empty value", context.getBean(ShelvedService).emptyValueSpeltOut())
        assertEquals(["unnamed shelf"], ShelfCalls.RECORDED)
    }

    @Test
    void leavesAloneAMethodBoundByAnotherValue() {
        assertEquals("named", context.getBean(ShelvedService).onANamedShelf())
        assertEquals([], ShelfCalls.RECORDED)
    }
}
