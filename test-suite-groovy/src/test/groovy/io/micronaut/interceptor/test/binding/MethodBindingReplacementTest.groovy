package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * A binding a method declares replaces the whole binding of the same type its class declares, as it does in Java.
 */
class MethodBindingReplacementTest {

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
        ZoneCalls.RECORDED.clear()
    }

    @Test
    void aMethodWithoutABindingOfItsOwnIsBoundByTheClass() {
        assertEquals("inherited", context.getBean(ZonedService).inherited())
        assertEquals(["zone a"], ZoneCalls.RECORDED)
    }

    @Test
    void aMethodBindingLeftToItsDefaultReplacesTheClassBinding() {
        assertEquals("by default", context.getBean(ZonedService).replacedByDefault())
        assertEquals(["zone z"], ZoneCalls.RECORDED)
    }
}
