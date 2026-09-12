package io.micronaut.interceptor.processor;

import io.micronaut.annotation.processing.test.JavaFileObjects;
import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A class named by {@code @Interceptors} is the same interceptor class whichever order the compilation visits it in.
 *
 * <p>An interceptor method that interposes on a lifecycle callback has the annotation that would make Micronaut
 * invoke it as a callback of its own class taken off it once it has been recorded, and the metadata of a method is
 * the same metadata wherever it is read from. A class whose only interceptor method is such a callback therefore
 * reads as declaring nothing once it has been visited, and the filter that leaves out a named class with nothing to
 * interpose with dropped it without a word - whenever the interceptor class happened to be visited before the class
 * naming it.</p>
 *
 * <p>What a class named by {@code @Interceptors} declares is read from the descriptor the processor records on it
 * instead, which says what it declared before anything was taken off it. The interception is asserted through the
 * proxy: the class naming the interceptor declares nothing else that would be intercepted, so Micronaut generates a
 * proxy of it exactly when the named interceptor class was kept.</p>
 */
class NamedInterceptorOrderTest {

    private static final String PACKAGE = "io.micronaut.interceptor.test.order";

    /**
     * An interceptor class whose only interceptor method interposes on a lifecycle callback. It is a bean of its
     * own, which is what puts it and the class naming it among the elements of the same annotation and so makes the
     * order they are visited in the order they are compiled in.
     */
    private static final String LIFECYCLE_ONLY = """
        package io.micronaut.interceptor.test.order;

        import jakarta.annotation.PostConstruct;
        import jakarta.inject.Singleton;
        import jakarta.interceptor.Interceptor;
        import jakarta.interceptor.InvocationContext;

        @Interceptor
        @Singleton
        public class LifecycleOnly {
            @PostConstruct
            public void created(InvocationContext context) throws Exception {
                context.proceed();
            }
        }
        """;

    private static final String CONSUMER = """
        package io.micronaut.interceptor.test.order;

        import jakarta.inject.Singleton;
        import jakarta.interceptor.Interceptors;

        @Singleton
        @Interceptors(LifecycleOnly.class)
        public class Consumer {
            public String work() {
                return "done";
            }
        }
        """;

    /**
     * An interceptor method of a superclass overridden by a method that is not one, which leaves the class with
     * nothing to interpose with: the case the filter was added for.
     */
    private static final String OVERRIDDEN = """
        package io.micronaut.interceptor.test.order;

        import jakarta.inject.Singleton;
        import jakarta.interceptor.InvocationContext;

        @Singleton
        public class Overridden extends OverriddenBase {
            @Override
            public Object around(InvocationContext context) throws Exception {
                return context.proceed();
            }
        }
        """;

    private static final String OVERRIDDEN_BASE = """
        package io.micronaut.interceptor.test.order;

        import jakarta.interceptor.AroundInvoke;
        import jakarta.interceptor.InvocationContext;

        public class OverriddenBase {
            @AroundInvoke
            public Object around(InvocationContext context) throws Exception {
                return context.proceed();
            }
        }
        """;

    private static final String OVERRIDDEN_CONSUMER = """
        package io.micronaut.interceptor.test.order;

        import jakarta.inject.Singleton;
        import jakarta.interceptor.Interceptors;

        @Singleton
        @Interceptors(Overridden.class)
        public class OverriddenConsumer {
            public String work() {
                return "done";
            }
        }
        """;

    /**
     * Compiles the sources in the order they are given and returns the names of the files the compilation produced.
     */
    private static List<String> compile(String... sources) {
        JavaFileObject[] files = new JavaFileObject[sources.length];
        for (int i = 0; i < sources.length; i++) {
            String source = sources[i];
            String name = source.substring(source.indexOf("public class ") + "public class ".length());
            files[i] = JavaFileObjects.forSourceString(PACKAGE + "." + name.split("[ \n{]")[0], source);
        }
        try (JavaParser parser = new JavaParser()) {
            List<String> produced = new ArrayList<>();
            parser.generate(files).forEach(file -> produced.add(file.getName()));
            return produced;
        }
    }

    private static boolean proxied(List<String> produced, String name) {
        return produced.stream().anyMatch(file -> file.endsWith("$" + name + "$Definition$Intercepted.class"));
    }

    @Test
    void aLifecycleOnlyNamedInterceptorIsKeptWhenItIsCompiledBeforeTheClassNamingIt() {
        List<String> produced = compile(LIFECYCLE_ONLY, CONSUMER);

        assertTrue(proxied(produced, "Consumer"), produced.toString());
    }

    @Test
    void aLifecycleOnlyNamedInterceptorIsKeptWhenItIsCompiledAfterTheClassNamingIt() {
        List<String> produced = compile(CONSUMER, LIFECYCLE_ONLY);

        assertTrue(proxied(produced, "Consumer"), produced.toString());
    }

    /**
     * Section 5.2 j): the interceptor method of the superclass is overridden by a method that is not one, so the
     * named class has nothing to interpose with and is left out - in either order, and although the superclass
     * records the method it declared.
     */
    @Test
    void aNamedClassWhoseOnlyInterceptorMethodIsOverriddenIsLeftOutWhateverTheOrder() {
        List<String> produced = compile(OVERRIDDEN_BASE, OVERRIDDEN, OVERRIDDEN_CONSUMER);
        assertFalse(proxied(produced, "OverriddenConsumer"), produced.toString());

        produced = compile(OVERRIDDEN_CONSUMER, OVERRIDDEN, OVERRIDDEN_BASE);
        assertFalse(proxied(produced, "OverriddenConsumer"), produced.toString());
    }
}
