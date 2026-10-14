package io.micronaut.interceptor.test.eager;

import io.micronaut.aop.Around;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.inject.qualifiers.Qualifiers;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3 creates every interceptor instance of an object when the object is created. The eager creation
 * the module does is covered for a proxy with an eager target (EagerCreationTest.methodOnlyProxyTarget); the same
 * shape with a lazily resolved target, whose methods are bound to separate interceptors, is the case here.
 */
class LazyTargetEagerCreationTest {

    static final List<String> CREATED = Collections.synchronizedList(new ArrayList<>());

    @Test
    void theEagerTargetCreatesBothInterceptorsWithTheTarget() {
        assertBothCreatedOnTheFirstCall(EagerTarget.class, bean -> ((EagerTarget) bean).first());
    }

    @Test
    void theLazyTargetCreatesBothInterceptorsWithTheTarget() {
        assertBothCreatedOnTheFirstCall(LazyTarget.class, bean -> ((LazyTarget) bean).first());
    }

    /**
     * Two lazy targets of one class, two beans of one {@code @EachBean}, are told apart by the bean Micronaut hands
     * the advice, rather than by looking among the definitions of the class, which finds both and cannot tell which.
     */
    @Test
    void eachLazyTargetOfOneClassCreatesBothInterceptorsWithTheTarget() {
        CREATED.clear();
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "test.lazy-targets.one.enabled", "true", "test.lazy-targets.two.enabled", "true"))) {
            LazyEachTarget one = context.getBean(LazyEachTarget.class, Qualifiers.byName("one"));
            assertEquals("first one", one.first());

            assertEquals(List.of("first", "second"), CREATED.stream().sorted().toList(),
                "the interceptor of the method not called is created with the target");
        }
    }

    private static void assertBothCreatedOnTheFirstCall(Class<?> type, java.util.function.Function<Object, String> call) {
        CREATED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            Object bean = context.getBean(type);
            assertEquals("first", call.apply(bean));

            assertEquals(List.of("first", "second"), CREATED.stream().sorted().toList(),
                type.getSimpleName() + ": the interceptor of the method not called is created with the target");
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface First {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Second {
    }

    @Interceptor
    @First
    public static class FirstInterceptor {

        public FirstInterceptor() {
            CREATED.add("first");
        }

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Interceptor
    @Second
    public static class SecondInterceptor {

        public SecondInterceptor() {
            CREATED.add("second");
        }

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Singleton
    @Around(proxyTarget = true)
    public static class EagerTarget {

        @First
        public String first() {
            return "first";
        }

        @Second
        public String second() {
            return "second";
        }
    }

    @EachProperty("test.lazy-targets")
    public static class LazyTargetConfiguration {

        private final String name;
        private boolean enabled;

        public LazyTargetConfiguration(@Parameter String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @EachBean(LazyTargetConfiguration.class)
    @Around(proxyTarget = true, lazy = true)
    public static class LazyEachTarget {

        private final String name;

        public LazyEachTarget(LazyTargetConfiguration configuration) {
            this.name = configuration.getName();
        }

        @First
        public String first() {
            return "first " + name;
        }

        @Second
        public String second() {
            return "second " + name;
        }
    }

    @Singleton
    @Around(proxyTarget = true, lazy = true)
    public static class LazyTarget {

        @First
        public String first() {
            return "first";
        }

        @Second
        public String second() {
            return "second";
        }
    }
}
