package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Two annotations carrying one binding that differ only in a member excluded from it carry the same binding, whether
 * or not either of them gives that member a value, as in Java.
 */
class ComposedNonbindingBindingTest {

    @Test
    fun bindsThroughTwoCarriersThatDifferOnlyInAMemberThatDoesNotBind() {
        ApplicationContext.run().use { context ->
            GradeCalls.recorded.clear()
            assertEquals("work", context.getBean(GradedService::class.java).work())
            assertEquals(listOf("graded work"), GradeCalls.recorded)
        }
    }
}
