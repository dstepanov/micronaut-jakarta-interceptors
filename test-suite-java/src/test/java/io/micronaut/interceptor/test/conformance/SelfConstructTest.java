package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 2.7 d) of the specification: an {@code @AroundConstruct} method is defined on an interceptor class, not
 * on the target class.
 *
 * <p>A class declaring one is read as an interceptor class, so the method never interposes on the construction of
 * the class itself - which it could not, there being no instance to invoke it on until the constructor has run.
 * The class remains an ordinary bean in every other respect.</p>
 */
class SelfConstructTest {

    @Test
    void anAroundConstructMethodDoesNotInterposeOnTheConstructionOfItsOwnClass() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();

            SelfConstructingService service = context.getBean(SelfConstructingService.class);

            assertEquals(List.of("constructed"), List.copyOf(Calls.RECORDED),
                "the constructor ran and the method declared beside it did not interpose on it");
            assertEquals("done", service.work(), "and the bean is usable as any other");
        }
    }
}
