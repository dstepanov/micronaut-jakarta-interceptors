package io.micronaut.interceptor.test

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinMicronautApiTest {

    @Test
    fun anInterceptorReadsTheInterceptionWithoutReflecting() {
        Calls.clear()
        ApplicationContext.run().use { context ->
            val service = context.createBean(CompiledService::class.java, "Denis")
            assertEquals("hello Denis", service.greet("hello"))

            assertEquals(
                listOf(
                    "AROUND_CONSTRUCT CompiledService(1) same=true",
                    "POST_CONSTRUCT CompiledService.started region=users",
                    "AROUND CompiledService.greet region=users parameters=[greeting]"
                ),
                Calls.recorded
            )
        }
    }
}
