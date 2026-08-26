package io.micronaut.interceptor.test.graalvm;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The same interception, run inside a GraalVM native image by the {@code nativeTest} task.
 *
 * <p>No reachability metadata is declared anywhere in this module. An interception that reached for the reflection
 * of the platform would fail here, so that this passes is what shows the chain, the bindings and the invocation of
 * the interceptor methods to be free of it.</p>
 */
class NativeImageInterceptionTest {

    @Test
    void interceptsInsideANativeImage() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("Hello replaced", context.getBean(TracedService.class).greet("Denis"));

            assertEquals(
                List.of("aroundConstruct", "postConstruct", "named", "aroundInvoke Denis"),
                Calls.RECORDED);
        }
    }
}
