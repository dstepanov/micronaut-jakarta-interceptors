package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A binding annotation declared on another annotation binds a function that annotation is declared on, as it does
 * in Java.
 */
class CarriedMethodBindingTest {

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
    fun clear() = WatchCalls.recorded.clear()

    @Test
    fun bindsTheFunctionAPlainAnnotationCarriesTheBindingTo() {
        val bean = context.getBean(SurveilledService::class.java)
        assertEquals("work", bean.work())
        assertEquals("rest", bean.rest())
        assertEquals(listOf("watched work"), WatchCalls.recorded)
    }
}
