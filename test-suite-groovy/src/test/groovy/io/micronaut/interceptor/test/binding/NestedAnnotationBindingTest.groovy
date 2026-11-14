package io.micronaut.interceptor.test.binding

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * A binding whose members hold annotations, one of them and an array of another, binds by the members of those that
 * bind, as it does in Java and Kotlin: a member of a held annotation declaring {@code @Nonbinding} is left out.
 */
class NestedAnnotationBindingTest {

    @Test
    void leavesTheMembersOfANestedAnnotationThatDoNotBindOutOfTheBinding() {
        ApplicationContext context = ApplicationContext.run()
        try {
            GovernCalls.RECORDED.clear()
            GovernedService service = context.getBean(GovernedService)
            assertEquals("work", service.work())
            assertEquals("other", service.other())
            assertEquals("other clause", service.otherClause())
            assertEquals(["governed work"], GovernCalls.RECORDED)
        } finally {
            context.close()
        }
    }
}
