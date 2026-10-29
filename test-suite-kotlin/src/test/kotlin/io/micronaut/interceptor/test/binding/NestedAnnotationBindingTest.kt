package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.ReflectionConfig
import io.micronaut.core.annotation.TypeHint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * A binding whose members hold annotations, one of them and an array of them, binds by the members of those that
 * bind, as in Java, and the processor permits building every one of those annotation types as an instance.
 */
class NestedAnnotationBindingTest {

    @Test
    fun leavesTheMembersOfANestedAnnotationThatDoNotBindOutOfTheBinding() {
        ApplicationContext.run().use { context ->
            GovernCalls.recorded.clear()
            val service = context.getBean(GovernedService::class.java)
            assertEquals("work", service.work())
            assertEquals("other", service.other())
            assertEquals(listOf("governed work"), GovernCalls.recorded)
        }
    }

    @Test
    fun permitsBuildingTheBindingAndTheAnnotationsItsMembersHold() {
        ApplicationContext.run().use { context ->
            val permitted = context.getBeanDefinition(GovernedService::class.java)
                .getAnnotationValuesByType(ReflectionConfig::class.java)
                .filter { it.enumValues("accessType", TypeHint.AccessType::class.java)
                    .contains(TypeHint.AccessType.DYNAMIC_PROXY) }
                .mapNotNull { it.stringValue("type").orElse(null) }
                .toSet()
            assertEquals(setOf(Governed::class.java.name, Rule::class.java.name), permitted)
        }
    }
}
