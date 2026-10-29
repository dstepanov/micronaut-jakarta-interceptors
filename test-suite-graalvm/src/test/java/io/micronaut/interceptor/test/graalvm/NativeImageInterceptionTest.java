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

    /**
     * A private interceptor method, which Micronaut reaches reflectively, together with the three things an
     * interceptor may ask the context for that are looked up with the reflection of the platform: the method, the
     * constructor and the binding annotations. The callbacks of a lifecycle event run in one chain, superclass
     * first, here as anywhere else.
     */
    @Test
    void reachesWhatNeedsReflectionInsideANativeImage() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(GuardedService.class).work());

            assertEquals(
                List.of("aroundConstruct GuardedService",
                    "postConstruct init",
                    "base",
                    "own",
                    "aroundInvoke work",
                    "binding Guarded"),
                Calls.RECORDED);
        }
    }

    /**
     * The members of a binding that return annotations, nested, defaulted or in an array, return instances built
     * as the binding is, for which the processor declares the metadata as well.
     */
    @Test
    void readsAnnotationsNestedInABindingInsideANativeImage() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(PolicyService.class).work());
            assertEquals(List.of("service noted fallback [first, second]"), Calls.RECORDED);
        }
    }

    /**
     * A binding declared only on the member of a factory producing a bean is built as an instance as well, the
     * annotation its member returns included.
     */
    @Test
    void readsABindingOnlyAFactoryMemberDeclaresInsideANativeImage() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ProducedService.class).work());
            assertEquals(List.of("produced factory"), Calls.RECORDED);
        }
    }
}
