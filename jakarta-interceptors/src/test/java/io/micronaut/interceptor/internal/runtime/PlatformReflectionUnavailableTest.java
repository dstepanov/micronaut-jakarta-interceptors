package io.micronaut.interceptor.internal.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An accessor of {@code InvocationContext} that returns an object of the Java reflection API reports a missing
 * {@code micronaut-reflection} as a missing dependency, and nothing else as one.
 *
 * <p>{@code test-suite-no-reflection} holds the first half inside an application without the module; this holds the
 * second, which needs a class to go missing that is not the module's.</p>
 */
class PlatformReflectionUnavailableTest {

    @Test
    void aClassOfTheModuleGoingMissingNamesTheDependency() {
        NoClassDefFoundError missing = new NoClassDefFoundError("io/micronaut/reflection/ReflectionExecutables");

        UnsupportedOperationException failure =
            AbstractInvocationContext.reflectionUnavailable("getMethod()", missing);

        assertTrue(failure.getMessage().contains("InvocationContext.getMethod()"), failure.getMessage());
        assertTrue(failure.getMessage().contains("io.micronaut:micronaut-reflection"), failure.getMessage());
        assertSame(missing, failure.getCause());
    }

    /**
     * Every use after the first fails saying that the class linking to the module could not be initialized, with
     * the first failure as its cause.
     */
    @Test
    void aLaterUseNamesTheDependencyAsWell() {
        NoClassDefFoundError later = new NoClassDefFoundError(
            "Could not initialize class " + PlatformReflection.class.getName());
        later.initCause(new ExceptionInInitializerError(
            "Exception java.lang.NoClassDefFoundError: io/micronaut/reflection/ReflectionExecutables"));

        UnsupportedOperationException failure =
            AbstractInvocationContext.reflectionUnavailable("getConstructor()", later);

        assertTrue(failure.getMessage().contains("InvocationContext.getConstructor()"), failure.getMessage());
        assertSame(later, failure.getCause());
    }

    @Test
    void anotherClassGoingMissingIsRethrownAsItIs() {
        NoClassDefFoundError missing = new NoClassDefFoundError("com/example/MissingMemberType");

        NoClassDefFoundError thrown = assertThrows(NoClassDefFoundError.class,
            () -> AbstractInvocationContext.reflectionUnavailable("getInterceptorBindings()", missing));

        assertSame(missing, thrown);
    }
}
