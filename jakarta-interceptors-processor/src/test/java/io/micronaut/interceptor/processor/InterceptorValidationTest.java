package io.micronaut.interceptor.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;

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
     * Two paths carrying the binding with different values of a member that is excluded from the binding, which is
     * no conflict: the two occurrences bind by the same thing, since the member they differ in takes no part in the
     * binding. Micronaut records an excluded member only where a value was supplied for it, so one of the two
     * occurrences said nothing about the exclusion and the values were compared as declared.
     */
    @Test
    void aBindingReachingAClassTwiceDifferingOnlyInAnExcludedMemberIsAccepted() {
        compileSuccessfully("""
            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @interface Baz {
                String value() default "shared";

                @io.micronaut.context.annotation.NonBinding
                String label() default "x";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @Baz
            @interface Foo {
            }

            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @Baz(label = "y")
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
     * The same two paths differing in a member that does take part in the binding, which is the conflict the
     * specification reports: the exclusion of one member says nothing about the rest.
     */
    @Test
    void aBindingReachingAClassTwiceDifferingInABindingMemberBesideAnExcludedOneIsReported() {
        String error = compile("""
            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @interface Baz {
                String value() default "shared";

                @io.micronaut.context.annotation.NonBinding
                String label() default "x";
            }

            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @Baz(value = "one", label = "x")
            @interface Foo {
            }

            @Retention(RetentionPolicy.RUNTIME)
            @InterceptorBinding
            @Baz(value = "two", label = "y")
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

    /**
     * An annotation that is not a binding itself still carries the bindings declared on it, so two of them may
     * disagree on a method as two binding annotations may.
     */
    @Test
    void aBindingReachingAMethodThroughTwoPlainAnnotationsWithDifferentValuesIsReported() {
        String error = compile("""
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
            @InterceptorBinding
            @interface Baz {
                String value();
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
            @Baz("yes")
            @interface Foo {
            }

            @Retention(RetentionPolicy.RUNTIME)
            @Target(ElementType.METHOD)
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
        assertTrue(error.contains("The @AroundInvoke method [intercept]")
            && error.contains("must accept a single jakarta.interceptor.InvocationContext"), error);
    }

    /**
     * Sections 2.6 and 2.7 give every interceptor method one parameter, an {@code InvocationContext}. A second
     * declaration that does not have it interposes on nothing, and the class satisfies every other check there is,
     * so it was left out of the chains of the interceptor without a word. The reference implementation refuses to
     * deploy such a class: Weld 7.0.0.CR1 reports WELD-001449, that the method "is not defined according to the
     * specification", for exactly this shape.
     */
    @Test
    void aMalformedInterceptorMethodBesideAValidOneIsReported() {
        String error = compile("""
            @Interceptor
            public class Subject {
                @AroundInvoke
                public Object intercept(InvocationContext context) throws Exception {
                    return context.proceed();
                }

                @AroundInvoke
                public Object bad(String value) {
                    return value;
                }
            }
            """);
        assertTrue(error.contains("The @AroundInvoke method [bad]")
            && error.contains("must accept a single jakarta.interceptor.InvocationContext"), error);
    }

    /**
     * The same for the kinds the specification gives the same signature, each of them beside a valid interceptor
     * method of the class.
     *
     * @return A test for each kind
     */
    @TestFactory
    List<DynamicTest> aMalformedDeclarationOfEveryKindIsReported() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String[] kind : new String[][]{
            {"AroundInvoke", "Object", "return null;"},
            {"AroundTimeout", "Object", "return null;"},
            {"AroundConstruct", "void", ""},
            {"PostConstruct", "void", ""},
            {"PreDestroy", "void", ""}}) {
            String annotation = kind[0];
            String returns = kind[1];
            String body = kind[2];
            tests.add(DynamicTest.dynamicTest("a malformed @" + annotation + " method", () -> {
                String error = compile("""
                    @Interceptor
                    public class Subject {
                        @AroundInvoke
                        public Object intercept(InvocationContext context) throws Exception {
                            return context.proceed();
                        }

                        @%1$s
                        public %2$s bad(String value) {
                            %3$s
                        }
                    }
                    """.formatted(annotation, returns, body));
                assertTrue(error.contains("The @" + annotation + " method [bad]")
                    && error.contains("must accept a single jakarta.interceptor.InvocationContext"), error);
            }));
        }
        return tests;
    }

    /**
     * Section 2.7: a lifecycle callback an interceptor class declares for its own lifecycle takes no argument, and
     * interposes on nothing. It is not a malformed interceptor method and has to keep compiling.
     */
    @Test
    void aCallbackOfTheInterceptorsOwnLifecycleIsAccepted() {
        compileSuccessfully("""
            @Interceptor
            public class Subject {
                @AroundInvoke
                public Object intercept(InvocationContext context) throws Exception {
                    return context.proceed();
                }

                @PostConstruct
                public void created() {
                }

                @PreDestroy
                public void destroyed() {
                }
            }
            """);
    }

    /**
     * A lifecycle callback of an ordinary bean is invoked by Micronaut with whatever it asks to have injected into
     * it, which is no business of this module: only the lifecycle callbacks of an interceptor class are read as
     * interposing on another object, and only those are held to the signature of the specification.
     */
    @Test
    void aLifecycleCallbackOfAnOrdinaryBeanWithAnInjectedParameterIsAccepted() {
        compileSuccessfully("""
            @Singleton
            public class Subject {
                @PostConstruct
                void created(Helper helper) {
                    helper.toString();
                }
            }

            @Singleton
            class Helper {
            }
            """);
    }

    /**
     * Section 3.3 f): a class carrying a class level binding may not be final, and may not declare a non-static
     * non-private final method. Both are reported: a final class by Micronaut, which refuses a class it cannot
     * generate a proxy of wherever the advice came from, and a final method by this module, which also counts the
     * ones Micronaut would pass over - see {@link FinalElementsTest}.
     */
    @Test
    void aFinalClassCarryingAClassLevelBindingIsReported() {
        String error = compile("""
            @InterceptorBinding
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD})
            @interface Guarded {
            }

            @Singleton
            @Guarded
            public final class Subject {
                public String work() {
                    return "done";
                }
            }
            """);
        assertTrue(error.contains("final class"), error);
    }

    @Test
    void aFinalMethodOfAClassCarryingAClassLevelBindingIsReported() {
        String error = compile("""
            @InterceptorBinding
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD})
            @interface Guarded {
            }

            @Singleton
            @Guarded
            public class Subject {
                public final String work() {
                    return "done";
                }
            }
            """);
        assertTrue(error.contains("declared final"), error);
    }

    /**
     * Sections 2.2 d), 2.7 i) and k), and 2.8 c) of the specification: a class declares at most one interceptor
     * method of a kind, and none of them may be static, final or abstract - for every kind, not only the
     * around-invoke one the tests above use. The checks are one path shared by the five kinds, parameterized only
     * by the annotation they name, so what each asserts is that the path reports the kind it was reached through.
     *
     * @return A test for each kind and each way of declaring its method wrongly
     */
    @TestFactory
    List<DynamicTest> everyKindOfInterceptorMethodIsValidatedAlike() {
        List<DynamicTest> tests = new ArrayList<>();
        // an around-timeout method returns what it interposed on; a construction or a lifecycle callback returns
        // nothing, which is the form its declaration most often takes
        for (String[] kind : new String[][]{
            {"AroundTimeout", "Object", "return context.proceed();"},
            {"AroundConstruct", "void", "context.proceed();"},
            {"PostConstruct", "void", "context.proceed();"},
            {"PreDestroy", "void", "context.proceed();"}}) {
            String annotation = kind[0];
            String returns = kind[1];
            String body = kind[2];

            tests.add(DynamicTest.dynamicTest("two @" + annotation + " methods", () -> {
                String error = compile("""
                    @Interceptor
                    public class Subject {
                        @%1$s
                        public %2$s one(InvocationContext context) throws Exception {
                            %3$s
                        }
                        @%1$s
                        public %2$s two(InvocationContext context) throws Exception {
                            %3$s
                        }
                    }
                    """.formatted(annotation, returns, body));
                assertTrue(error.contains("more than one @" + annotation + " method"), error);
            }));

            for (String modifier : new String[]{"static", "final"}) {
                tests.add(DynamicTest.dynamicTest("a " + modifier + " @" + annotation + " method", () -> {
                    String error = compile("""
                        @Interceptor
                        public class Subject {
                            @%1$s
                            public %4$s %2$s intercept(InvocationContext context) throws Exception {
                                %3$s
                            }
                        }
                        """.formatted(annotation, returns, body, modifier));
                    assertTrue(error.contains("The @" + annotation + " method [intercept]")
                        && error.contains("must not be static, final or abstract"), error);
                }));
            }

            tests.add(DynamicTest.dynamicTest("an abstract @" + annotation + " method", () -> {
                String error = compile("""
                    @Interceptor
                    public abstract class Subject {
                        @%1$s
                        public abstract %2$s intercept(InvocationContext context) throws Exception;
                    }
                    """.formatted(annotation, returns));
                assertTrue(error.contains("The @" + annotation + " method [intercept]")
                    && error.contains("must not be static, final or abstract"), error);
            }));
        }
        return tests;
    }

    /**
     * Two binding annotations declared on each other, and a third binding one of them carries. The cycle has to end,
     * and what the annotations bind through it has to be the same however they are reached.
     */
    private static final String CYCLIC_BINDINGS = """
        @InterceptorBinding
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        @interface Level {
            int value();
        }

        @InterceptorBinding
        @Level(1)
        @Second
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        @interface First {
        }

        @InterceptorBinding
        @First
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        @interface Second {
        }

        @InterceptorBinding
        @First
        @Level(2)
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        @interface Overriding {
        }

        @InterceptorBinding
        @Second
        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE, ElementType.METHOD})
        @interface ThroughSecond {
        }
        """;

    /**
     * Binding annotations may be declared on each other, and following them has to end rather than go round.
     */
    @Test
    void bindingAnnotationsDeclaredOnEachOtherCompile() {
        compileSuccessfully(CYCLIC_BINDINGS + """
            @First
            @Singleton
            public class Subject {
                public String greet() {
                    return "hello";
                }
            }
            """);
        compileSuccessfully(CYCLIC_BINDINGS + """
            @Second
            @Singleton
            public class Subject {
                public String greet() {
                    return "hello";
                }
            }
            """);
    }

    /**
     * Section 3.4.2 d) through a cycle. Overriding carries First and declares Level(2) itself, so it binds Level
     * with 2; ThroughSecond carries Second, which carries First, so it binds Level with 1. A class declaring both is
     * bound by Level twice with different values.
     *
     * <p>It was reported only when ThroughSecond was declared first. Declared the other way round, working out
     * Overriding reached First, whose cycle through Second closed on First and left Second worked out as binding
     * nothing; that answer was remembered, ThroughSecond read it, and the conflict went unreported. The two orders
     * are asserted apart because the order is what the defect depended on.</p>
     */
    @Test
    void aConflictReachedThroughACycleIsReportedWhateverTheOrder() {
        for (String annotations : new String[]{"@Overriding @ThroughSecond", "@ThroughSecond @Overriding"}) {
            String error = compile(CYCLIC_BINDINGS + """
                %s
                @Singleton
                public class Subject {
                    public String greet() {
                        return "hello";
                    }
                }
                """.formatted(annotations));
            assertTrue(error.contains("is bound by") && error.contains("Level"),
                "declared as " + annotations + ": " + error);
        }
    }
}