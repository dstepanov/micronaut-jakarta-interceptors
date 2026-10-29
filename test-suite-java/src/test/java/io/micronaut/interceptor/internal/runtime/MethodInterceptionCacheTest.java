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

        @Noted("#{1 + 1}")
        public String noted() {
            return "noted";
        }
    }

    /** Holds an evaluated expression, which has nothing to do with the interception of the method it is on. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    public @interface Noted {
        String value();
    }

    @Test
    void theMethodKeepsWhatItRecordsAndEveryBeanFindsItsChainUnderIt() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            CALLS.clear();
            assertEquals("worked", context.getBean(KeptPrototype.class).work());
            long read = settled(resolver);

            for (int i = 0; i < 100; i++) {
                assertEquals("worked", context.getBean(KeptPrototype.class).work());
            }

            assertEquals(read, settled(resolver), "no bean reads the interception again");
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

    /**
     * An evaluated expression on another annotation of a method is computed for each invocation, and has Micronaut
     * compute a value it keeps by the evaluated metadata again for every call. What the method records of its
     * interception is read beneath it, and is kept all the same.
     */
    @Test
    void aMethodWithAnEvaluatedExpressionKeepsWhatItRecordsAsWell() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ExecutableMethod<KeptPrototype, Object> noted =
                context.getBeanDefinition(KeptPrototype.class).getRequiredMethod("noted");
            assertTrue(noted.getAnnotationMetadata().hasEvaluatedExpressions(), "the method has an expression");
            CALLS.clear();
            assertEquals("noted", context.getBean(KeptPrototype.class).noted());
            assertEquals("noted", context.getBean(KeptPrototype.class).noted());

            assertEquals(List.of("created", "invoke noted", "created", "invoke noted"), CALLS);
            MethodInterception kept = noted.getCachedUnevaluatedValue(MethodInterception.class, method -> {
                throw new AssertionError("the method keeps what it records although it has an expression");
            });
            assertSame(kept, noted.getCachedUnevaluatedValue(MethodInterception.class, method -> {
                throw new AssertionError("the method keeps what it records although it has an expression");
            }));
        }
    }

    /**
     * The count once the beans the scheduler creates on a thread of its own have been resolved, which is the count
     * holding still across consecutive reads.
     */
    private static long settled(InterceptorChainResolver resolver) {
        long previous = -1;
        int stable = 0;
        for (int attempt = 0; attempt < 100; attempt++) {
            long current = resolver.readInterceptions();
            stable = current == previous ? stable + 1 : 0;
            if (stable == 5) {
                return current;
            }
            previous = current;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the count to settle", e);
            }
        }
        throw new AssertionError("The count never settled, last seen " + previous);
    }
}
