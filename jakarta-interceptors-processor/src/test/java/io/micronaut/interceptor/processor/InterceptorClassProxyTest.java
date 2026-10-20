package io.micronaut.interceptor.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which classes get a proxy where interception and interceptor classes meet in one hierarchy. An interceptor class
 * is not intercepted whichever path it is reached by: declared as a bean, produced by a factory, or extending a class
 * that is intercepted. A class is intercepted by what it declares or inherits, and a binding that is not inherited -
 * one declared on an interface, or one declared on a superclass without {@code @Inherited} - is not something it
 * inherits.
 */
class InterceptorClassProxyTest {

    private static final String PACKAGE = "io.micronaut.interceptor.test.proxies";

    private static String source(String body) {
        return """
            package io.micronaut.interceptor.test.proxies;

            import io.micronaut.context.annotation.Factory;
            import jakarta.inject.Singleton;
            import jakarta.interceptor.AroundInvoke;
            import jakarta.interceptor.Interceptor;
            import jakarta.interceptor.InterceptorBinding;
            import jakarta.interceptor.InvocationContext;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Inherited;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @InterceptorBinding
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD})
            @interface Guarded {
            }

            @Inherited
            @InterceptorBinding
            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.TYPE, ElementType.METHOD})
            @interface Lineage {
            }

            @Interceptor
            @Guarded
            class GuardedInterceptor {
                @AroundInvoke
                Object around(InvocationContext context) throws Exception {
                    return context.proceed();
                }
            }

            %s
            """.formatted(body);
    }

    /**
     * Compiles the source and returns the simple names of the proxies Micronaut generated.
     */
    private static List<String> proxies(String body) {
        try (JavaParser parser = new JavaParser()) {
            Iterable<? extends JavaFileObject> files = parser.generate(PACKAGE + ".Subject", source(body));
            return StreamSupport.stream(files.spliterator(), false)
                .map(JavaFileObject::getName)
                .filter(name -> name.endsWith("$Intercepted.class"))
                .map(name -> name.substring(name.lastIndexOf('/') + 1))
                .sorted()
                .toList();
        }
    }

    /**
     * The superclass is proxied as well: a class declaring advice is a bean in Micronaut, and the binding it declares
     * is advice once the processor has declared the interception of it.
     */
    @Test
    void anInterceptorClassExtendingAnInterceptedClassIsNotProxied() {
        List<String> proxies = proxies("""
            @Lineage
            class Base {
                public String inherited() {
                    return "inherited";
                }

                protected String guardedByTheClass() {
                    return "protected";
                }

                @Guarded
                public String guarded() {
                    return "guarded";
                }
            }

            @Singleton
            class Subject extends Base {
            }

            @Interceptor
            class LineageInterceptor extends Base {
                @AroundInvoke
                Object around(InvocationContext context) throws Exception {
                    return context.proceed();
                }
            }
            """);
        assertEquals(List.of("$Base$Definition$Intercepted.class", "$Subject$Definition$Intercepted.class"), proxies);
    }

    @Test
    void anInterceptorClassAFactoryProducesIsNotProxied() {
        List<String> proxies = proxies("""
            @Factory
            class Subject {
                @Singleton
                GuardedInterceptor interceptor() {
                    return new GuardedInterceptor();
                }
            }
            """);
        assertEquals(List.of(), proxies);
    }

    @Test
    void aBeanExtendingAnInterceptorClassIsProxiedToInterceptItself() {
        List<String> proxies = proxies("""
            @Singleton
            class Subject extends GuardedInterceptor {
                public String work() {
                    return "work";
                }
            }
            """);
        assertEquals(List.of("$Subject$Definition$Intercepted.class"), proxies);
    }

    @Test
    void theImplementationOfAnInterfaceCarryingABindingIsNotProxied() {
        List<String> proxies = proxies("""
            @Guarded
            interface Api {
                String work();

                @Guarded
                String guarded();
            }

            @Singleton
            class Subject implements Api {
                @Override
                public String work() {
                    return "work";
                }

                @Override
                public String guarded() {
                    return "guarded";
                }
            }
            """);
        assertEquals(List.of(), proxies);
    }

    @Test
    void aSubclassOfAnAbstractClassCarryingABindingThatIsNotInheritedIsProxiedForItsBoundMethodsAlone() {
        List<String> proxies = proxies("""
            @Guarded
            abstract class Base {
                public String work() {
                    return "work";
                }
            }

            @Singleton
            class Subject extends Base {
            }

            @Singleton
            class BoundSubject extends Base {
                @Guarded
                public String bound() {
                    return "bound";
                }
            }
            """);
        assertEquals(List.of("$BoundSubject$Definition$Intercepted.class"), proxies);
    }

    @Test
    void aFactoryProducingAnInterfaceCarryingABindingProducesAProxy() {
        List<String> proxies = proxies("""
            @Guarded
            interface Api {
                String work();
            }

            @Factory
            class Subject {
                @Singleton
                Api api() {
                    return () -> "work";
                }
            }
            """);
        assertFalse(proxies.isEmpty(), proxies.toString());
        assertTrue(proxies.stream().allMatch(name -> name.startsWith("$Subject$Api")), proxies.toString());
    }
}
