package io.micronaut.interceptor.test

import io.micronaut.context.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SuspendInterceptionTest {

    /**
     * A suspending function is intercepted like any other: the arguments it declares are the ones the interceptor
     * sees and may replace, and the continuation the Kotlin compiler adds after them is not shown.
     */
    @Test
    fun interceptsASuspendingFunction() {
        ApplicationContext.run().use { context ->
            Calls.clear()

            val result = runBlocking { context.getBean(SuspendingService::class.java).suspending("Denis") }

            // the continuation the Kotlin compiler adds is not one of the parameters, and replacing the one
            // parameter the function declares reaches it
            assertEquals("Hello replaced", result)
            assertEquals(listOf("parameters [Denis]", "body"), Calls.recorded)
        }
    }
}
