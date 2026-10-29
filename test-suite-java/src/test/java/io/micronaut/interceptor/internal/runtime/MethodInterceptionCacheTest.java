package io.micronaut.interceptor.internal.runtime;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.metadata.InterceptionKind;
import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interception a method records is read once and kept by the method, and the chain of every bean created of a
 * prototype is found under the key kept there.
 */
class MethodInterceptionCacheTest {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    public @interface Kept {
    }

    @Interceptor
    @Kept
    public static class KeptInterceptor {
        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            CALLS.add("invoke " + context.getMethod().getName());
            return context.proceed();
        }

        @PostConstruct
        public void created(InvocationContext context) throws Exception {
            CALLS.add("created");
            context.proceed();
        }
    }

    @Prototype
    @Kept
    public static class KeptPrototype {
        public String work() {
            return "worked";
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    public @interface Described {
        String value();
    }

    @Prototype
    @Kept
    public static class EvaluatedPrototype {
        @Described("#{ 'evaluated' }")
        public String work() {
            return "worked";
        }
    }

    @Test
    void aMethodWhoseMetadataHoldsAnEvaluatedExpressionKeepsWhatItRecordsAsWell() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("worked", context.getBean(EvaluatedPrototype.class).work());
            assertEquals("worked", context.getBean(EvaluatedPrototype.class).work());

            ExecutableMethod<EvaluatedPrototype, Object> work =
                context.getBeanDefinition(EvaluatedPrototype.class).getRequiredMethod("work");
            assertTrue(work.getAnnotationMetadata().hasEvaluatedExpressions());
            MethodInterception kept = work.getCachedUnevaluatedValue(MethodInterception.class, method -> {
                throw new AssertionError("the interception is read from the compiled metadata alone, and kept");
            });
            assertSame(kept, work.getCachedUnevaluatedValue(MethodInterception.class, MethodInterception::of));
        }
    }

    @Test
    void theMethodKeepsWhatItRecordsAndEveryBeanFindsItsChainUnderIt() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            CALLS.clear();
            assertEquals("worked", context.getBean(KeptPrototype.class).work());
            long read = resolver.readInterceptions();

            for (int i = 0; i < 100; i++) {
                assertEquals("worked", context.getBean(KeptPrototype.class).work());
            }

            assertEquals(read, resolver.readInterceptions(), "no bean reads the interception again");
            assertEquals(202, CALLS.size(), "each bean is created and invoked through its chain");
            ExecutableMethod<KeptPrototype, Object> work =
                context.getBeanDefinition(KeptPrototype.class).getRequiredMethod("work");
            MethodInterception kept = work.getCachedUnevaluatedValue(MethodInterception.class, method -> {
                throw new AssertionError("the method keeps what it records once it has been invoked");
            });
            assertSame(kept.key(InterceptionKind.AROUND_INVOKE), kept.key(InterceptionKind.AROUND_INVOKE));
            assertEquals(List.of(KeptInterceptor.class), resolver.resolve(io.micronaut.aop.InterceptorKind.AROUND, work, false)
                .stream().<Class<?>>map(InterceptorReference::interceptorClass).toList());
        }
    }
}
