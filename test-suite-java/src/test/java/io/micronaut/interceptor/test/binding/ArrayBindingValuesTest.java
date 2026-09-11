package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An array-valued member is compared by its contents, and an array of empty strings is as different from an empty
 * array as any other array of a different length is.
 */
class ArrayBindingValuesTest {

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
    void bindsAnEmptyArrayToAnEmptyArray() {
        assertEquals("none", context.getBean(TaggedService.class).withNoTags());

        assertEquals(List.of("no tags"), Calls.RECORDED);
    }

    @Test
    void leavesAloneAMethodBoundByAnArrayOfOneEmptyString() {
        assertEquals("one empty", context.getBean(TaggedService.class).withOneEmptyTag());

        assertEquals(List.of(), Calls.RECORDED);
    }

    @Test
    void leavesAloneAMethodBoundByAnArrayOfTwoEmptyStrings() {
        assertEquals("two empty", context.getBean(TaggedService.class).withTwoEmptyTags());

        assertEquals(List.of(), Calls.RECORDED);
    }
}
