package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * A binding annotation declared on another annotation binds a method that annotation is declared on, as it does in
 * Java.
 */
class CarriedMethodBindingTest {

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
        WatchCalls.RECORDED.clear()
    }

    @Test
    void bindsTheMethodAPlainAnnotationCarriesTheBindingTo() {
        SurveilledService bean = context.getBean(SurveilledService)
        assertEquals("work", bean.work())
        assertEquals("rest", bean.rest())
        assertEquals(["watched work"], WatchCalls.RECORDED)
    }
}
