package io.micronaut.interceptor.test.repeatable;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A repeatable binding binds by every occurrence of it an element carries, not by one of them.
 *
 * <p>An interceptor is bound to an element when every binding the interceptor declares is a binding of the element,
 * and an occurrence of a repeatable binding is one binding: an interceptor declaring {@code @Tag("one")} is bound to
 * an element carrying {@code @Tag("one")} and {@code @Tag("two")}, and one declaring both is not bound to an element
 * carrying only the first. That is what the reference implementation does - Weld 7.0.0.CR1 intercepts in the first
 * case and does not in the second - and the order the occurrences are declared in says nothing.</p>
 */
class RepeatedBindingTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @BeforeEach
    void clear() {
        Calls.clear();
    }

    @Test
    void bindsAnInterceptorDeclaringOneOfTheOccurrencesOfTheElement() {
        assertEquals("done", context.getBean(BothTagsService.class).work());

        assertEquals(List.of("both", "one", "work"), Calls.RECORDED);
    }

    /**
     * An occurrence is compared by the members that take part in the binding, as a single declaration is: the
     * interceptor says nothing about the member excluded from the binding and the occurrences of the element each
     * say something different, and the one they agree on binds them.
     */
    @Test
    void bindsAnOccurrenceThatDiffersOnlyInAnExcludedMember() {
        assertEquals("done", context.getBean(NotedService.class).work());

        assertEquals(List.of("noted", "work"), Calls.RECORDED);
    }

    @Test
    void doesNotBindAnInterceptorDeclaringAnOccurrenceTheElementDoesNotCarry() {
        assertEquals("done", context.getBean(OneTagService.class).work());

        assertEquals(List.of("one", "work"), Calls.RECORDED);
    }
}
