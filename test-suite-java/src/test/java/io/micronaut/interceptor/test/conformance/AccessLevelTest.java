package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessLevelTest {

    /**
     * 2.6 cc)/cd), 2.7 f) and 2.7 ia): a protected and a package private interceptor method are both invoked, one
     * method interposes on both lifecycle events, and what it returns is ignored.
     */
    @Test
    void invokesInterceptorMethodsThatAreNotPublic() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(LevelsService.class).work());
            assertEquals(
                java.util.List.of("package private callback", "protected aroundInvoke"),
                Calls.RECORDED);
            Calls.clear();
        }
        assertEquals(java.util.List.of("package private callback"), Calls.RECORDED,
            "2.7 f) one method interposes on both lifecycle events");
    }
}
