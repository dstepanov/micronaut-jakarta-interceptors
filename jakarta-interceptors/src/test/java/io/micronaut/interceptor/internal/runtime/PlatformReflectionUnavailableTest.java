package io.micronaut.interceptor.internal.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An accessor of {@code InvocationContext} that returns an object of the Java reflection API reports a missing
 * {@code micronaut-reflection} as a missing dependency.
 *
 * <p>This module compiles against the reflection module without depending on it, so its own tests run without it,
 * as an application without it does. {@code test-suite-no-reflection} holds the same inside such an application; this
 * holds how the absence is found out: by looking for the module once, rather than by recognising the failure of
 * whatever happened to use it, so that a class of the application going missing is never taken for the module.</p>
 */
class PlatformReflectionUnavailableTest {

    @Test
    void theModuleIsAbsentHere() {
        assertNull(PlatformReflectionUnavailableTest.class.getClassLoader()
            .getResource("io/micronaut/reflection/ReflectionExecutables.class"));
    }

    @Test
    void anAccessorThatNeedsTheModuleNamesTheDependency() {
        UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
            () -> AbstractInvocationContext.requireReflectionModule("getMethod()"));

        assertTrue(failure.getMessage().contains("InvocationContext.getMethod()"), failure.getMessage());
        assertTrue(failure.getMessage().contains("io.micronaut:micronaut-reflection"), failure.getMessage());
        NoClassDefFoundError cause = assertInstanceOf(NoClassDefFoundError.class, failure.getCause());
        assertTrue(cause.getMessage().contains("ReflectionExecutables"), cause.getMessage());
    }

    /**
     * The module is looked for once, and every later accessor fails for the same reason, naming itself.
     */
    @Test
    void theModuleIsLookedForOnce() {
        UnsupportedOperationException first = assertThrows(UnsupportedOperationException.class,
            () -> AbstractInvocationContext.requireReflectionModule("getInterceptorBindings()"));
        UnsupportedOperationException later = assertThrows(UnsupportedOperationException.class,
            () -> AbstractInvocationContext.requireReflectionModule("getInterceptorBinding(Class)"));

        assertNotNull(AbstractInvocationContext.ReflectionModule.MISSING);
        assertSame(AbstractInvocationContext.ReflectionModule.MISSING, first.getCause());
        assertSame(first.getCause(), later.getCause());
        assertTrue(later.getMessage().contains("InvocationContext.getInterceptorBinding(Class)"), later.getMessage());
    }
}
