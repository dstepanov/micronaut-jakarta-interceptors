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

    private static String source(String body) {
        return """
            package io.micronaut.interceptor.test.invalid;

            import jakarta.annotation.PostConstruct;
            import jakarta.annotation.PreDestroy;
            import jakarta.interceptor.AroundConstruct;
            import jakarta.interceptor.AroundInvoke;
            import jakarta.interceptor.AroundTimeout;
            import jakarta.inject.Singleton;
            import jakarta.interceptor.Interceptor;
            import jakarta.interceptor.InterceptorBinding;
            import jakarta.interceptor.InvocationContext;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

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

    private static void compileSuccessfully(String body) {
        try (JavaParser parser = new JavaParser()) {
            parser.generate(PACKAGE + ".Subject", source(body));
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
        assertTrue(error.contains("must not be static, final or abstract"), error);
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
        assertTrue(error.contains("must not be static, final or abstract"), error);
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
        compileSuccessfully("""
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

    /**
     * The conflict the specification reports on a class is one wherever a binding is declared. A method carries
     * its own bindings, and two of its annotations may disagree there just as they may on a class.
     */
    @Test
    void aBindingReachingAMethodTwiceWithDifferentValuesIsReported() {
        String error = compile("""
            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @interface Baz {
                String value();
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
            @InterceptorBinding
            @Baz("yes")
            @interface Foo {
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
            @InterceptorBinding
            @Baz("no")
            @interface Bar {
            }

            @Singleton
            public class Subject {
                @Foo
                @Bar
                public String greet() {
                    return "hello";
                }
            }
            """);
        assertTrue(error.contains("The method") && error.contains("is bound by") && error.contains("Baz"), error);
    }

    @Test
    void aBindingReachingAConstructorTwiceWithDifferentValuesIsReported() {
        String error = compile("""
            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @interface Baz {
                String value();
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
            @InterceptorBinding
            @Baz("yes")
            @interface Foo {
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
            @InterceptorBinding
            @Baz("no")
            @interface Bar {
            }

            @Singleton
            public class Subject {
                @Foo
                @Bar
                public Subject() {
                }

                public String greet() {
                    return "hello";
                }
            }
            """);
        assertTrue(error.contains("The constructor") && error.contains("is bound by") && error.contains("Baz"), error);
    }

    /**
     * A conflict on the class is the class's own and is reported once, on the class. The metadata of a method is
     * read together with the metadata of its class, so a method must not be made to report it again - nor a method
     * of a class that settled the disagreement by declaring the binding itself.
     */
    @Test
    void aBindingTheClassSettlesIsNotReportedOnItsMethods() {
        compileSuccessfully("""
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
            @Baz("settled")
            @Singleton
            public class Subject {
                public String greet() {
                    return "hello";
                }
            }
            """);
    }

    @Test
    void anAbstractInterceptorMethodIsReported() {
        String error = compile("""
            @Interceptor
            public abstract class Subject {
                @AroundInvoke
                public abstract Object intercept(InvocationContext context) throws Exception;
            }
            """);
        assertTrue(error.contains("must not be static, final or abstract"), error);
    }

    /**
     * The specification gives an {@code @AroundInvoke} method the signature {@code Object <METHOD>(InvocationContext)}
     * exactly: one returning something narrower cannot return what it interposed on.
     */
    @Test
    void anInterceptorMethodThatReturnsSomethingOtherThanObjectIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @AroundInvoke
                public String intercept(InvocationContext context) throws Exception {
                    return String.valueOf(context.proceed());
                }
            }
            """);
        assertTrue(error.contains("must return Object"), error);
    }

    @Test
    void aLifecycleInterceptorMethodThatReturnsSomethingOtherThanVoidOrObjectIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @PostConstruct
                public String created(InvocationContext context) throws Exception {
                    return String.valueOf(context.proceed());
                }
            }
            """);
        assertTrue(error.contains("must return void or Object"), error);
    }

    /**
     * {@code void} and {@code Object} are both signatures the specification gives a lifecycle callback and an
     * {@code @AroundConstruct} interceptor method; the second is what lets one method interpose on business
     * methods as well.
     */
    @Test
    void aLifecycleInterceptorMethodReturningVoidOrObjectIsAccepted() {
        compileSuccessfully("""
            @Interceptor
            public class Subject {
                @PostConstruct
                public Object created(InvocationContext context) throws Exception {
                    return context.proceed();
                }

                @PreDestroy
                public void destroyed(InvocationContext context) throws Exception {
                    context.proceed();
                }

                @AroundConstruct
                public void constructed(InvocationContext context) throws Exception {
                    context.proceed();
                }
            }
            """);
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
