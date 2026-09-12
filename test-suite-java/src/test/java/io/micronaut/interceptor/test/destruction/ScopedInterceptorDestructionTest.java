package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3 a): an interceptor with a scope of its own belongs to that scope and not to any one object it intercepts, and
 * a custom scope is no different from {@code @Singleton} there. Micronaut resolves a bean of a custom scope through a
 * proxy it creates for whoever asked, and reports that proxy as a dependent, so the thing that tells these instances
 * apart has to look past it to the bean the scope keeps.
 */
class ScopedInterceptorDestructionTest {

    @Test
    void leavesAScopedInterceptorToItsScope() {
        Destructions.RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            ScopedUsingPrototype first = context.getBean(ScopedUsingPrototype.class);
            ScopedUsingPrototype second = context.getBean(ScopedUsingPrototype.class);
            assertEquals("done", first.work());
            Destructions.RECORDED.clear();

            context.destroyBean(first);

            // the interceptor is the scope's, and the other object is still intercepted by it
            assertEquals(List.of(), Destructions.RECORDED);
            assertEquals("done", second.work());
            assertEquals(List.of("scoped around"), Destructions.RECORDED);
            Destructions.RECORDED.clear();
        }
        // the scope closes with the context, and the interceptor goes then
        assertEquals(List.of("scoped interceptor destroyed"), Destructions.RECORDED);
    }
}
