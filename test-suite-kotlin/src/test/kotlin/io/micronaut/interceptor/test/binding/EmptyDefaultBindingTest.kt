package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A member that defaults to an empty string or an empty array is filled in before the comparison as any other
 * default is, as it is in Java.
 */
class EmptyDefaultBindingTest {

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
    fun clear() = ShelfCalls.recorded.clear()

    @Test
    fun bindsTheDefaultsLeftOutOnBothSides() {
        assertEquals("defaults", context.getBean(ShelvedService::class.java).leftToItsDefaults())
        assertEquals(listOf("unnamed shelf"), ShelfCalls.recorded)
    }

    @Test
    fun bindsEveryEmptyDefaultSpeltOutToTheDefaultsLeftOut() {
        assertEquals("every default", context.getBean(ShelvedService::class.java).everyDefaultSpeltOut())
        assertEquals(listOf("unnamed shelf"), ShelfCalls.recorded)
    }

    @Test
    fun leavesAloneAFunctionBoundByAnotherValue() {
        assertEquals("named", context.getBean(ShelvedService::class.java).onANamedShelf())
        assertEquals(listOf<String>(), ShelfCalls.recorded)
    }
}
