package io.micronaut.interceptor.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What becomes of a {@code final} class or method that a binding reaches. A proxy of the class is a subclass of it,
 * so a final class cannot have one and a final method cannot be overridden by one. Section 3.3 f) makes a final
 * class carrying a class level binding, and a non-static non-private final method of such a class - declared or
 * inherited - a definition error, and both are reported as the class is compiled. A final method that declares a
 * binding of its own is the one case left out of the proxy rather than reported, which section 3.3 g) records as
 * a deliberate difference.
 *
 * <p>A final class carrying a class level binding, and a public final method declared by such a class, are in
 * {@link InterceptorValidationTest}, which the conformance page cites for section 3.3 f).</p>
 */
class FinalElementsTest {

    private static final String PACKAGE = "io.micronaut.interceptor.test.invalid";
    private static final String PROXY = PACKAGE + ".$Subject$Definition$Intercepted";

    private static String source(String body) {
        return """
            package io.micronaut.interceptor.test.invalid;

            import jakarta.inject.Singleton;
            import jakarta.interceptor.AroundInvoke;
            import jakarta.interceptor.Interceptor;
            import jakarta.interceptor.InterceptorBinding;
            import jakarta.interceptor.InvocationContext;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @InterceptorBinding
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD})
            @interface Guarded {
            }

            @Interceptor
            @Guarded
            class GuardedInterceptor {
                @AroundInvoke
                Object around(InvocationContext context) throws Exception {
                    return context.proceed();
                }
            }

            class Base {
                public final String inherited() {
                    return "base";
                }
            }

            %s
            """.formatted(body);
    }

    private static String compile(String body) {
        try (JavaParser parser = new JavaParser()) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                () -> parser.generate(PACKAGE + ".Subject", source(body)));
            return failure.getMessage();
        }
    }

    /**
     * Compiles the source and returns the names of the methods the generated proxy of {@code Subject} overrides,
     * or {@code null} when no proxy was generated.
     */
    private static List<String> proxiedMethods(String body) throws Exception {
        try (JavaParser parser = new JavaParser()) {
            Iterable<? extends JavaFileObject> files = parser.generate(PACKAGE + ".Subject", source(body));
            boolean proxied = StreamSupport.stream(files.spliterator(), false)
                .anyMatch(file -> file.getName().endsWith("$Subject$Definition$Intercepted.class"));
            if (!proxied) {
                return null;
            }
            Class<?> proxy = new GeneratedClassLoader(files).loadClass(PROXY);
            Class<?> subject = proxy.getSuperclass();
            return Arrays.stream(proxy.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> Arrays.stream(subject.getMethods()).anyMatch(m -> m.getName().equals(name))
                    || Arrays.stream(subject.getDeclaredMethods()).anyMatch(m -> m.getName().equals(name)))
                .sorted()
                .toList();
        }
    }

    /**
     * Defines the classes a compilation produced, reading each from what the compiler wrote.
     */
    private static final class GeneratedClassLoader extends ClassLoader {

        private final Iterable<? extends JavaFileObject> files;

        GeneratedClassLoader(Iterable<? extends JavaFileObject> files) {
            super(FinalElementsTest.class.getClassLoader());
            this.files = files;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String path = "/" + name.replace('.', '/') + ".class";
            for (JavaFileObject file : files) {
                if (file.getName().endsWith(path)) {
                    try (InputStream in = file.openInputStream()) {
                        byte[] bytes = in.readAllBytes();
                        return defineClass(name, bytes, 0, bytes.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
            }
            throw new ClassNotFoundException(name);
        }
    }

    @Test
    void aFinalClassWithABindingOnOneOfItsMethodsIsReported() {
        String error = compile("""
            @Singleton
            public final class Subject {
                @Guarded
                public String work() {
                    return "done";
                }
            }
            """);
        assertTrue(error.contains("Cannot apply AOP advice to final class"), error);
    }

    @Test
    void aPackagePrivateFinalMethodOfAClassCarryingAClassLevelBindingIsReported() {
        String error = compile("""
            @Singleton
            @Guarded
            public class Subject {
                final String work() {
                    return "done";
                }
            }
            """);
        assertTrue(error.contains("method [work], declared final by [io.micronaut.interceptor.test.invalid.Subject]"), error);
    }

    /**
     * A protected method is not one Micronaut advises for a class level binding, so Micronaut would pass a final one
     * over; the specification does not.
     */
    @Test
    void aProtectedFinalMethodOfAClassCarryingAClassLevelBindingIsReported() {
        String error = compile("""
            @Singleton
            @Guarded
            public class Subject {
                protected final String work() {
                    return "done";
                }

                public String other() {
                    return "other";
                }
            }
            """);
        assertTrue(error.contains("method [work], declared final by [io.micronaut.interceptor.test.invalid.Subject]"), error);
    }

    /**
     * Micronaut refuses a public final method only where the class declares it, and passes over one the class
     * inherits from a superclass; the specification counts the inherited one too.
     */
    @Test
    void anInheritedFinalMethodOfAClassCarryingAClassLevelBindingIsReported() {
        String error = compile("""
            @Singleton
            @Guarded
            public class Subject extends Base {
                public String work() {
                    return "done";
                }
            }
            """);
        assertTrue(error.contains("method [inherited], declared final by [io.micronaut.interceptor.test.invalid.Base]"), error);
    }

    /**
     * The final methods every class inherits from {@code Object} are not methods of the class the specification
     * has in mind, and a private or static final method is not one a proxy would have to override.
     */
    @Test
    void aPrivateOrStaticFinalMethodOfAClassCarryingAClassLevelBindingCompiles() throws Exception {
        assertEquals(List.of("work"), proxiedMethods("""
            @Singleton
            @Guarded
            public class Subject {
                private final String hidden() {
                    return "hidden";
                }

                static final String shared() {
                    return "shared";
                }

                public String work() {
                    return hidden() + shared();
                }
            }
            """));
    }

    /**
     * Section 3.3 g) asks for a definition error here, and this module differs deliberately, as the conformance
     * page records: a final method is not a business method the processor can intercept, so the binding it
     * declares is not turned into advice, and nothing is left for Micronaut to refuse. The class is not proxied
     * when that method is all that was bound.
     */
    @Test
    void aFinalMethodWithABindingOfItsOwnCompilesAndIsNotIntercepted() throws Exception {
        assertEquals(null, proxiedMethods("""
            @Singleton
            public class Subject {
                @Guarded
                public final String work() {
                    return "done";
                }
            }
            """));
    }

    @Test
    void aFinalMethodWithABindingOfItsOwnIsLeftOutOfTheProxyOfItsClass() throws Exception {
        assertEquals(List.of("other"), proxiedMethods("""
            @Singleton
            public class Subject {
                @Guarded
                public final String work() {
                    return "done";
                }

                @Guarded
                public String other() {
                    return "other";
                }
            }
            """));
    }
}
