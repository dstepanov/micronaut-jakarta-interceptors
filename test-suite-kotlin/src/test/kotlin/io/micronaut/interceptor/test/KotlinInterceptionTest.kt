package io.micronaut.interceptor.test

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class KotlinInterceptionTest {

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
    fun clear() = Calls.clear()

    @Test
    fun interceptsThroughABindingAnnotation() {
        assertEquals("Hello Denis", context.getBean(GreetingService::class.java).greet("Denis"))
        assertEquals(listOf("log in greet", "greet", "log out"), Calls.recorded)
    }

    @Test
    fun interceptsThroughAnInterceptorNamedDirectly() {
        context.getBean(GreetingService::class.java).counted()
        // an interceptor named directly runs before one bound by an annotation, whatever its priority
        assertEquals(listOf("count", "log in counted", "counted", "log out"), Calls.recorded)
    }
}
