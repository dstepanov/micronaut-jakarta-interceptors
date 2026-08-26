package io.micronaut.interceptor.test

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class GroovyInterceptionTest {

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
        Calls.clear()
    }

    @Test
    void interceptsThroughABindingAnnotation() {
        assertEquals("Hello Denis", context.getBean(GreetingService).greet("Denis"))
        assertEquals(["log in greet", "greet", "log out"], Calls.RECORDED)
    }

    @Test
    void interceptsThroughAnInterceptorNamedDirectly() {
        context.getBean(GreetingService).counted()
        // an interceptor named directly runs before one bound by an annotation, whatever its priority
        assertEquals(["count", "log in counted", "counted", "log out"], Calls.RECORDED)
    }
}
