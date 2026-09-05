package io.micronaut.interceptor.test

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class GroovyMicronautApiTest {

    @Test
    void anInterceptorReadsTheInterceptionWithoutReflecting() {
        Calls.clear()
        try (def context = ApplicationContext.run()) {
            def service = context.createBean(CompiledService, "Denis")
            assertEquals("hello Denis", service.greet("hello"))

            assertEquals([
                "AROUND_CONSTRUCT CompiledService(1) same=true",
                "POST_CONSTRUCT CompiledService.started region=users",
                "AROUND CompiledService.greet region=users parameters=[greeting]"
            ], Calls.RECORDED)
        }
    }
}
