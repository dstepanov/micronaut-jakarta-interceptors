package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A binding a function declares replaces the whole binding of the same type its class declares, as it does in Java.
 */
class MethodBindingReplacementTest {

    companion object {

        private lateinit var context: ApplicationContext

        @JvmStatic
        @BeforeAll
        fun startContext() {
            context = ApplicationContext.run()
        }

        @JvmStatic
        @AfterAll
        fun stopContext() {
            context.close()
        }
    }

    @BeforeEach
    fun clear() = ZoneCalls.recorded.clear()

    @Test
    fun aFunctionWithoutABindingOfItsOwnIsBoundByTheClass() {
        assertEquals("inherited", context.getBean(ZonedService::class.java).inherited())
        assertEquals(listOf("zone a"), ZoneCalls.recorded)
    }

    @Test
    fun aFunctionBindingLeftToItsDefaultReplacesTheClassBinding() {
        assertEquals("by default", context.getBean(ZonedService::class.java).replacedByDefault())
        assertEquals(listOf("zone z"), ZoneCalls.recorded)
    }
}
