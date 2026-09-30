package io.micronaut.interceptor.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 3.1.1 c) an interceptor binding type declared {@code @Target(TYPE)} may not be applied to an
 * interceptor binding type declared {@code @Target({TYPE, METHOD})}. The Java compiler accepts the declaration: a
 * TYPE annotation may annotate an annotation type. So it is the processor that has to report it.
 */
class BindingTargetCompatibilityTest {

    @Test
    void aTypeOnlyBindingOnATypeAndMethodBindingIsReported() {
        try (JavaParser parser = new JavaParser()) {
            RuntimeException error = assertThrows(RuntimeException.class, () -> parser.generate("io.micronaut.interceptor.test.invalid.Subject", """
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
                @Target(ElementType.TYPE)
                @interface TypeOnly {
                }

                @InterceptorBinding
                @TypeOnly
                @Retention(RetentionPolicy.RUNTIME)
                @Target({ElementType.TYPE, ElementType.METHOD})
                @interface ForMethods {
                }

                @Interceptor
                @ForMethods
                class ForMethodsInterceptor {
                    @AroundInvoke
                    Object intercept(InvocationContext context) throws Exception {
                        return context.proceed();
                    }
                }

                @Singleton
                public class Subject {
                    @ForMethods
                    public String work() {
                        return "done";
                    }
                }
                """), "the declaration of @ForMethods is a definition error, and it compiled");
            assertTrue(String.valueOf(error.getMessage()).contains("which may not be written on [METHOD]"),
                "reported as a binding target error: " + error.getMessage());
        }
    }
}
