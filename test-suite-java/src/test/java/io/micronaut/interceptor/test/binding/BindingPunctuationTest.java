package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What a binding is compared by is written out as one string at compilation time, and two bindings that differ
 * must not be written the same way however the punctuation of that string falls inside their values.
 */
class BindingPunctuationTest {

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
    void leavesAloneAMethodWhoseBindingOnlyReadsLikeTheInterceptors() {
        assertEquals("different", context.getBean(PunctuatedService.class).looksLikeTheInterceptor());

        assertEquals(List.of(), Calls.RECORDED);
    }

    @Test
    void interceptsTheMethodBoundByTheSameValues() {
        assertEquals("same", context.getBean(PunctuatedService.class).isTheInterceptor());

        assertEquals(List.of("punctuated"), Calls.RECORDED);
    }
}
