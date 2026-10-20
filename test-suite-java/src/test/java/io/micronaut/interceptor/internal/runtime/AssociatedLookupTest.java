package io.micronaut.interceptor.internal.runtime;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The interceptor classes associated with a construction are the same for every kind of interception, so the ones
 * a set of bindings binds are looked for once for the association rather than once for every kind.
 */
class AssociatedLookupTest {

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, CONSTRUCTOR, METHOD})
    public @interface Zone {
        String value();
    }

    @Interceptor
    @Zone("business")
    public static class BusinessZoneInterceptor {
        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Interceptor
    @Zone("construction")
    public static class ConstructionZoneInterceptor {
        @AroundConstruct
        public Object construct(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Singleton
    @Zone("business")
    public static class ZonedService {
        @Zone("construction")
        public ZonedService() {
        }

        public String work() {
            return "worked";
        }
    }

    @Test
    void looksForTheInterceptorsOfEachSetOfBindingsOnce() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InterceptorChainResolver resolver = context.getBean(InterceptorChainResolver.class);
            // the constructor declares a binding of its own that replaces the one of the class, so the association
            // is built from two sets of bindings
            AnnotationMetadata constructor = context.getBeanDefinition(ZonedService.class)
                .getConstructor()
                .getAnnotationMetadata();

            long before = settled(resolver);
            List<InterceptorReference> associated = resolver.resolveAssociated(constructor);
            assertEquals(2, resolver.boundInterceptorLookups() - before, "one lookup for each set of bindings");
            assertEquals(Set.of(ConstructionZoneInterceptor.class, BusinessZoneInterceptor.class),
                associated.stream().map(InterceptorReference::interceptorClass).collect(Collectors.toSet()));

            long resolved = settled(resolver);
            assertSame(associated, resolver.resolveAssociated(constructor));
            assertEquals(resolved, resolver.boundInterceptorLookups(), "a repeated lookup looks for nothing");
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
            long current = resolver.boundInterceptorLookups();
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
