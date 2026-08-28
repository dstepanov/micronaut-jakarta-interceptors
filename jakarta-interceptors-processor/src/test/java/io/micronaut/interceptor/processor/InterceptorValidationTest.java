package io.micronaut.interceptor.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The declarations the specification does not allow are reported when they are compiled, rather than left to fail
 * at runtime.
 */
class InterceptorValidationTest {

    private static final String PACKAGE = "io.micronaut.interceptor.test.invalid";

    private static String compile(String body) {
        try (JavaParser parser = new JavaParser()) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                () -> parser.generate(PACKAGE + ".Subject", """
                    package io.micronaut.interceptor.test.invalid;

                    import jakarta.annotation.PostConstruct;
                    import jakarta.interceptor.AroundConstruct;
                    import jakarta.interceptor.AroundInvoke;
                    import jakarta.interceptor.AroundTimeout;
                    import jakarta.inject.Singleton;
                    import jakarta.interceptor.Interceptor;
                    import jakarta.interceptor.InterceptorBinding;
                    import jakarta.interceptor.InvocationContext;
                    import java.lang.annotation.Retention;
                    import java.lang.annotation.RetentionPolicy;

                    %s
                    """.formatted(body)));
            return failure.getMessage();
        }
    }

    @Test
    void anInterceptorClassWithoutAnInterceptorMethodIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
            }
            """);
        assertTrue(error.contains("declares no interceptor method"), error);
    }

    @Test
    void anInterceptorMethodThatReturnsNothingIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @AroundInvoke
                public void intercept(InvocationContext context) throws Exception {
                    context.proceed();
                }
            }
            """);
        assertTrue(error.contains("must return Object"), error);
    }

    @Test
    void twoInterceptorMethodsOfTheSameKindAreReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @AroundInvoke
                public Object one(InvocationContext context) throws Exception {
                    return context.proceed();
                }
                @AroundInvoke
                public Object two(InvocationContext context) throws Exception {
                    return context.proceed();
                }
            }
            """);
        assertTrue(error.contains("more than one @AroundInvoke method"), error);
    }

    @Test
    void aFinalInterceptorMethodIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @AroundInvoke
                public final Object intercept(InvocationContext context) throws Exception {
                    return context.proceed();
                }
            }
            """);
        assertTrue(error.contains("must not be static or final"), error);
    }

    @Test
    void aStaticInterceptorMethodIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @AroundInvoke
                public static Object intercept(InvocationContext context) throws Exception {
                    return context.proceed();
                }
            }
            """);
        assertTrue(error.contains("must not be static or final"), error);
    }

    @Test
    void aTimeoutInterceptorMethodThatReturnsNothingIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @AroundTimeout
                public void intercept(InvocationContext context) throws Exception {
                    context.proceed();
                }
            }
            """);
        assertTrue(error.contains("must return Object"), error);
    }

    /**
     * The shape of the {@code bindings/broken} deployment of the kit, which a container is required to reject:
     * two binding annotations declared together, each declaring the same third binding, with a different value.
     */
    @Test
    void aBindingReachingAClassTwiceWithDifferentValuesIsReported() {
        String error = compile("""
            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @interface Baz {
                String value();
            }

            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @Baz("yes")
            @interface Foo {
            }

            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @Baz("no")
            @interface Bar {
            }

            @Foo
            @Bar
            @Singleton
            public class Subject {
                public String greet() {
                    return "hello";
                }
            }
            """);
        assertTrue(error.contains("is bound by") && error.contains("Baz"), error);
    }

    /**
     * The same two paths carrying the same value, which is not a conflict and has to keep compiling.
     */
    @Test
    void aBindingReachingAClassTwiceWithTheSameValueIsAccepted() {
        try (JavaParser parser = new JavaParser()) {
            parser.generate(PACKAGE + ".Subject", """
                package io.micronaut.interceptor.test.invalid;

                import jakarta.inject.Singleton;
                import jakarta.interceptor.InterceptorBinding;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;

                @Retention(RetentionPolicy.RUNTIME)
                @InterceptorBinding
                @interface Baz {
                    String value();
                }

                @Retention(RetentionPolicy.RUNTIME)
                @InterceptorBinding
                @Baz("yes")
                @interface Foo {
                }

                @Retention(RetentionPolicy.RUNTIME)
                @InterceptorBinding
                @Baz("yes")
                @interface Bar {
                }

                @Foo
                @Bar
                @Singleton
                public class Subject {
                    public String greet() {
                        return "hello";
                    }
                }
                """);
        }
    }

    @Test
    void anInterceptorMethodThatTakesTheWrongParameterIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @AroundInvoke
                public Object intercept(String wrong) throws Exception {
                    return null;
                }
            }
            """);
        assertTrue(error.contains("declares no interceptor method"), error);
    }
}
